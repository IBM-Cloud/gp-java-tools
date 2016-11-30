/*
 * Copyright IBM Corp. 2015
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.g11n.pipeline.resfilter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.BreakIterator;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ibm.g11n.pipeline.resfilter.ResourceString.ResourceStringComparator;

/**
 *
 * @author Farhan Arshad
 *
 */
public class AndroidStringsResource implements ResourceFilter {

    private static final String CHAR_SET = "UTF-8";

    private static final String RESOURCES_STRING = "resources";
    private static final String NAME_STRING = "name";
    private static final String STR_STRING = "string";
    private static final String STR_ARRAY = "string-array";

    private static final String STR_ARRAY_OPEN_TAG_PTRN = "^(\\s*<string-array\\s*name=\".*\">).*";
    private static final String STR_ARRAY_CLOSE_TAG_PTRN = ".*(\\s*</string-array\\s*>)$";

    private static final String STR_OPEN_TAG_PTRN = "^(\\s*<string\\s*name=\".*\">).*";
    private static final String STR_CLOSE_TAG_PTRN = ".*(\\s*</string\\s*>)$";

    @Override
    public Bundle parse(InputStream in) throws IOException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // runtime problem
            throw new RuntimeException(e);
        }

        Document document = null;
        try {
            document = builder.parse(in);
        } catch (SAXException e) {
            throw new IllegalResourceFormatException(e);
        }

        Element elem = document.getDocumentElement();
        NodeList nodeList = elem.getChildNodes();
        List<ResourceString> resStrings = new LinkedList<>();
        collectResourceStrings(nodeList, 1 /* the first sequence number */, resStrings);

        return new Bundle(resStrings, null);
    }

    /**
     * This method traverses through the DOM tree and collect resource strings
     *
     * @param nodeList
     *            NodeList object
     * @param startSeqNum
     *            The first sequence number to be used
     * @param resStrings
     *            Collection to store result resource strings
     * @return The last sequence number + 1
     */
    private int collectResourceStrings(NodeList nodeList, int startSeqNum, Collection<ResourceString> resStrings) {
        int seqNum = startSeqNum;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            // looking for DOM element <string name=$NAME>VALUE</string>
            String nodeName = node.getNodeName();
            if (nodeName.equals(STR_STRING) || nodeName.equals(STR_ARRAY)) {
                String key = node.getAttributes().getNamedItem(NAME_STRING).getNodeValue();
                String value = node.getTextContent();

                // turn into array format, i.e. [vale1, value2]
                if (nodeName.equals(STR_ARRAY)) {
                    value = "[" + value.trim().replaceAll("\\n[ \t]+", ", ") + "]";
                }

                resStrings.add(new ResourceString(key, value, seqNum++));
            } else {
                seqNum = collectResourceStrings(node.getChildNodes(), seqNum, resStrings);
            }
        }
        return seqNum;
    }

    @Override
    public void write(OutputStream os, String language, Bundle resource) {

        TreeSet<ResourceString> sortedResources = new TreeSet<>(new ResourceStringComparator());
        sortedResources.addAll(resource.getResourceStrings());

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // runtime problem
            throw new RuntimeException(e);
        }

        // root elements
        Document doc = docBuilder.newDocument();

        // creating <resources></resources>
        Element rootElement = doc.createElement(RESOURCES_STRING);

        for (ResourceString key : sortedResources) {
            String value = key.getValue();

            if (value.startsWith("[") && value.endsWith("]")) {
                // creating <string-array name="$NAME">
                Element child = doc.createElement(STR_ARRAY);
                Attr attr = doc.createAttribute(NAME_STRING);
                attr.setValue(key.getKey());
                child.setAttributeNode(attr);

                int startIndex = 0;
                int endIndex = -1;

                while (endIndex < value.length() - 1) {
                    endIndex = value.indexOf(',', startIndex);

                    if (endIndex == -1) {
                        endIndex = value.length() - 1;
                    }

                    String itemValue = value.substring(startIndex + 1, endIndex);

                    Element arrayChild = doc.createElement("item");
                    arrayChild.setTextContent(itemValue);
                    child.appendChild(arrayChild);

                    startIndex = endIndex + 1;
                }
                rootElement.appendChild(child);
            } else {
                // creating <string name=$NAME>VALUE</string>
                Element child = doc.createElement(STR_STRING);
                Attr attr = doc.createAttribute(NAME_STRING);
                attr.setValue(key.getKey());
                child.setAttributeNode(attr);
                child.setTextContent(value);
                rootElement.appendChild(child);
            }

        }
        doc.appendChild(rootElement);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            // runtime problem
            throw new RuntimeException(e);
        }

        // to add the tab spacing to files
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(os);

        // write the file
        try {
            transformer.transform(source, result);
        } catch (TransformerException e) {
            // runtime problem?
            throw new RuntimeException(e);
        }
    }

    @Override
    public void merge(InputStream base, OutputStream outStream, String language, Bundle resource)
            throws IOException {
        // put res data into a map for easier searching
        Map<String, String> resMap = new HashMap<String, String>(resource.getResourceStrings().size() * 4 / 3 + 1);
        for (ResourceString res : resource.getResourceStrings()) {
            resMap.put(res.getKey(), res.getValue());
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(base, CHAR_SET));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream, CHAR_SET));

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.matches(STR_ARRAY_OPEN_TAG_PTRN)) {
                // handle <string-array name="name"> tag
                String openingTag = line.substring(0, line.indexOf('>') + 1);
                String key = openingTag.substring(openingTag.indexOf('"') + 1, openingTag.lastIndexOf('"'));

                if (!resMap.containsKey(key)) {
                    writer.write(line);
                    writer.newLine();
                    continue;
                }

                String value = resMap.get(key);

                if (!(value.startsWith("[") && value.endsWith("]"))) {
                    writer.write(line);
                    writer.newLine();
                    continue;
                }

                String tabSubString = openingTag.substring(0, openingTag.indexOf('<'));
                String spaces = tabSubString + getTabStr(tabSubString);
                writer.write(openingTag);
                writer.newLine();

                String[] items = value.substring(1, value.length() - 1).split(",");

                for (int i = 0; i < items.length; i++) {
                    writer.write(formatMessage("<item>", items[i].trim(), "</item>", spaces, language));
                }

                writer.write(openingTag.substring(0, openingTag.indexOf('<')));

                writer.write("</string-array>");
                writer.newLine();

                while ((line = reader.readLine()) != null && !line.matches(STR_ARRAY_CLOSE_TAG_PTRN))
                    ;
            } else if (line.matches(STR_OPEN_TAG_PTRN)) {
                // handle <string name="name"> tag
                String openingTag = line.substring(0, line.indexOf('>') + 1);
                String key = openingTag.substring(openingTag.indexOf('"') + 1, openingTag.lastIndexOf('"'));

                if (!resMap.containsKey(key)) {
                    writer.write(line);
                    writer.newLine();
                    continue;
                }

                String value = resMap.get(key);

                String spaces = openingTag.substring(0, openingTag.indexOf('<'));

                writer.write(formatMessage(openingTag.trim(), value, "</string>", spaces, language));

                while (line != null && !line.matches(STR_CLOSE_TAG_PTRN)) {
                    line = reader.readLine();
                }
            } else {
                writer.write(line);
                writer.newLine();
            }
        }

        writer.flush();
    }

    /**
     * This method looks at the provided string to determine if a tab char or
     * spaces are being used for tabbing.
     *
     * Defaults to spaces;
     */
    static String getTabStr(String str) {
        if (!str.isEmpty() && str.charAt(0) == '\t') {
            return "\t";
        } else {
            return "    ";
        }
    }

    /**
     * Gets the number of spaces the whitespace string is using. Tab chars are
     * equal to 4 chars. i.e. a tab is considered to be of size 4.
     */
    static int getSpacesSize(String whitespace) {
        int size = 0;
        for (int i = 0; i < whitespace.length(); i++) {
            if (whitespace.charAt(i) == '\t') {
                size += 4;
            } else if (whitespace.charAt(i) == ' ') {
                size++;
            }
        }
        return size;
    }

    static String formatMessage(String openingTag, String message, String closingTag, String whitespace,
            String localeStr) {
        int maxLineLen = 80;

        StringBuilder output = new StringBuilder();

        int messageLen = message.length();

        output.append(whitespace).append(openingTag);

        // message fits on one line
        if (maxLineLen > getSpacesSize(whitespace) + openingTag.length() + messageLen + closingTag.length()) {
            return output.append(message).append(closingTag).append('\n').toString();
        }

        // message needs to be split onto multiple lines
        output.append('\n');

        // word breaks differ based on the locale
        Locale locale;
        if (localeStr == null) {
            locale = Locale.getDefault();
        } else {
            locale = new Locale(localeStr);
        }
        BreakIterator wordIterator = BreakIterator.getWordInstance(locale);
        wordIterator.setText(message);

        // the available char space once we account for the tabbing
        // spaces and other chars such as quotes
        int available = maxLineLen - getSpacesSize(whitespace) - 4;

        String tabStr = getTabStr(whitespace);

        // a word iterator is used to traverse the message;
        // a reference to the previous word break is kept
        // so that once the current reference goes beyond
        // the available char limit, the message can be split
        // without going over the limit
        int start = 0;
        int end = wordIterator.first();
        int prevEnd = end;
        while (end != BreakIterator.DONE) {
            prevEnd = end;
            end = wordIterator.next();
            if (end - start > available) {
                output.append(whitespace).append(tabStr).append(message.substring(start, prevEnd)).append('\n');
                start = prevEnd;
            } else if (end == messageLen) {
                output.append(whitespace).append(tabStr).append(message.substring(start, end)).append('\n');
            }
        }

        return output.append(whitespace).append(closingTag).append('\n').toString();
    }
}