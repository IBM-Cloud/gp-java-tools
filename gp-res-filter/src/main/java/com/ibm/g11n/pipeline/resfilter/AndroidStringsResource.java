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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.BreakIterator;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Scanner;
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

public class AndroidStringsResource implements ResourceFilter {

    private static final String RESOURCES_STRING = "resources";
    private static final String NAME_STRING = "name";
    private static final String STR_STRING = "string";
    private static final String STR_ARRAY = "string-array";

    @Override
    public Collection<ResourceString> parse(InputStream in) throws IOException {

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
        Collection<ResourceString> resultCol = new LinkedList<ResourceString>();
        collectResourceStrings(nodeList, 1 /* the first sequence number */, resultCol);

        return resultCol;
    }

    /**
     * This method traverses through the DOM tree and collect resource
     * strings
     *
     * @param nodeList      NodeList object
     * @param startSeqNum   The first sequence number to be used
     * @param resStrings    Collection to store result resource strings
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
    public void write(OutputStream os, String language,
            Collection<ResourceString> map) {

        TreeSet<ResourceString> sortedResources = new TreeSet<>(new ResourceStringComparator());
        sortedResources.addAll(map);

        DocumentBuilderFactory docFactory = DocumentBuilderFactory
                .newInstance();
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

                while (endIndex < value.length()-1) {
                    endIndex = value.indexOf(',', startIndex);

                    if (endIndex == -1) {
                        endIndex = value.length()-1;
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

        TransformerFactory transformerFactory = TransformerFactory
                .newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            // runtime problem
            throw new RuntimeException(e);
        }

        // to add the tab spacing to files
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(
                "{http://xml.apache.org/xslt}indent-amount", "2");

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
    public void merge(InputStream base, OutputStream outStream, String language,
            Collection<ResourceString> data) throws IOException {
        Scanner in = new Scanner(base, "UTF-8");
        String line = "";
        String pattern = "^.*<string.*name=\".*\">.*\n";

        while (in.hasNextLine()) {
            line = in.nextLine() + "\n";

            if (!line.matches(pattern)) {
                outStream.write(line.getBytes());
            } else {
                String[] wordList = line.split("\"");
                String key = wordList[1].trim();

                // TODO: build hash map first, instead of
                // linear search in the given collection
                // every time?
                for (ResourceString res : data) {
                    if (res.getKey().equals(key)) {
                        StringBuilder temp = new StringBuilder(100);
                        final int character_offset = 80;

                        BreakIterator b = BreakIterator.getWordInstance();
                        b.setText(res.getValue());

                        int offset = 80;
                        int start = 0;

                        boolean first = true;

                        String whiteSpaceStr = line.substring(0, line.indexOf("<"));
                        temp.append(whiteSpaceStr).append("<string name=\"").append(key).append("\">");

                        while (start < res.getValue().length()) {
                            if (res.getValue().length() > character_offset) {

                                if (!first) {
                                    temp.append(whiteSpaceStr).append(" ");
                                }

                                first = false;
                                int end = b.following(offset);
                                String str = res.getValue().substring(start, end);
                                start = end;
                                offset += 80;
                                temp.append(str).append(" \\\n");
                            } else {
                                temp.append(res.getValue());
                                start = res.getValue().length();
                            }
                        }

                        if (res.getValue().length() > character_offset) {
                            temp.append(whiteSpaceStr);
                        }
                        temp.append("</string>\n");
                        outStream.write(temp.toString().getBytes());

                        while (line.indexOf("</string>") == -1) {
                            line = in.nextLine();
                        }
                    }
                }

            }
        }
        in.close();
    }
}
