/*
 * Copyright IBM Corp. 2015, 2017
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.BreakIterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XLIFFResource implements ResourceFilter {

    private static final String VERSION_STRING = "version";
    private static final String VERSION_NUMBER_STRING = "1.2";
    private static final String XMLNS_STRING = "xmlns:xsi";
    private static final String XMLNS_VALUE_STRING = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String XSI_STRING = "xsi:schemaLocation";
    private static final String XSI_VALUE_STRING = "urn:oasis:names:tc:xliff:document:1.2 xliff-core-1.2-strict.xsd";

    private static final String UNIT_STRING = "trans-unit";
    private static final String ID_STRING = "id";
    private static final String SOURCE_STRING = "source";
    private static final String TARGET_STRING = "target";
    private static final String XLIFF_STRING = "xliff";
    private static final String FILE_STRING = "file";
    private static final String ORIGINAL_STRING = "original";
    private static final String GLOBAL_STRING = "g11n-pipeline";
    private static final String DATATYPE_STRING = "datatype";
    private static final String PLAINTEXT_STRING = "plaintext";
    private static final String SOURCE_LANGUAGE_STRING = "source-language";
    private static final String ENGLISH = "en";
    private static final String TARGET_LANGUAGE_STRING = "target-language";
    private static final String BODY_STRING = "body";

    @Override
    public Bundle parse(InputStream in) throws FileNotFoundException, IOException {

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

        int version = (int) Float.parseFloat(elem.getAttribute(VERSION_STRING));

        Bundle bundle = new Bundle();
        collectResourceStrings(nodeList, 1 /* the first sequence number */, bundle, version, "");

        return bundle;
    }

    private int collectResourceStrings(NodeList nodeList, int startSeqNum, Bundle bundle, int version, String key) {
        int seqNum = startSeqNum;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeName().lastIndexOf(UNIT_STRING) != -1) {
                String newkey = node.getAttributes().getNamedItem(ID_STRING).getNodeValue();
                seqNum = collectResourceStrings(node.getChildNodes(), seqNum, bundle, version, newkey);
            } else if (node.getNodeName().equals(SOURCE_STRING)) {
                String value = node.getTextContent().replaceAll("\\s*\n\\s*", " ");
                bundle.addResourceString(key, value, seqNum++);
                return seqNum;
            } else {
                seqNum = collectResourceStrings(node.getChildNodes(), seqNum, bundle, version, key);
            }
        }
        return seqNum;
    }

    @Override
    public void write(OutputStream os, String language, Bundle bundle) {
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

        Element xliff = doc.createElement(XLIFF_STRING);
        xliff.setAttribute(VERSION_STRING, VERSION_NUMBER_STRING);
        xliff.setAttribute(XMLNS_STRING,XMLNS_VALUE_STRING);
        xliff.setAttribute(XSI_STRING,XSI_VALUE_STRING);

        Element file = doc.createElement(FILE_STRING);
        file.setAttribute(ORIGINAL_STRING, GLOBAL_STRING);
        file.setAttribute(DATATYPE_STRING, PLAINTEXT_STRING);
        file.setAttribute(SOURCE_LANGUAGE_STRING, ENGLISH);
        file.setAttribute(TARGET_LANGUAGE_STRING, language);
        xliff.appendChild(file);

        Element body = doc.createElement(BODY_STRING);
        file.appendChild(body);

        for (ResourceString key : bundle.getResourceStrings()) {
            Element trans_unit = doc.createElement(UNIT_STRING);
            trans_unit.setAttribute(ID_STRING, key.getKey());
            Element source = doc.createElement(SOURCE_STRING);
            source.setTextContent(key.getSrcValue());
            trans_unit.appendChild(source);
            Element target = doc.createElement(TARGET_STRING);
            target.setTextContent(key.getValue());
            trans_unit.appendChild(target);
            body.appendChild(trans_unit);
        }

        doc.appendChild(xliff);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            // runtime problem
            throw new RuntimeException(e);
        }

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(os);

        try {
            transformer.transform(source, result);
        } catch (TransformerException e) {
            // runtime problem?
            throw new RuntimeException(e);
        }
    }

    @Override
    @Deprecated
    /*
     * This method is incomplete and may not produce the expected output.
     *
     * (non-Javadoc)
     * @see com.ibm.g11n.pipeline.resfilter.ResourceFilter#merge(java.io.InputStream, java.io.OutputStream, java.lang.String, java.util.Collection)
     */
    public void merge(InputStream base, OutputStream os, String language, Bundle bundle) throws IOException {
        Map<String, String> resMap = new HashMap<String, String>(bundle.getResourceStrings().size() * 4/3 + 1);
        for (ResourceString res : bundle.getResourceStrings()) {
            resMap.put(res.getKey(), res.getValue());
        }

        // TODO: We should use xml encoding declaration, instead of hardcoding
        // "UTF-8"
        Scanner in = new Scanner(base, "UTF-8");
        String line = "";
        String key_pattern = "^.*<unit.*id=\".*\">\n$";
        String end_source_pattern = ".*</source>\n$";
        while (in.hasNextLine()) {
            line = in.nextLine() + '\n';
            if (!line.matches(key_pattern)) {
                if (line.indexOf("<xliff") != -1) {
                    StringBuilder newline = new StringBuilder(100);
                    newline.append("<xliff version=\"2.0\" srcLang=\"en\" targLang=\"").append(language).append("\">");
                    line = newline.toString();
                }
                os.write(line.getBytes());
            } else {
                String[] wordList = line.split("\"");
                String key = wordList[1];

                // +2 for source and source block
                String whiteSpace = line.substring(0, line.indexOf("<")) + "  ";

                // writes key line
                while (!line.matches(end_source_pattern)) {
                    os.write(line.getBytes());
                    line = in.nextLine() + '\n';
                }

                os.write(line.getBytes());
                // TODO: Instead of linear search resource key every time,
                // we may create hash map first.

                if (resMap.containsKey(key)) {
                    String value = resMap.get(key);
                    final int character_offset = 80;

                    BreakIterator b = BreakIterator.getWordInstance();
                    b.setText(value);

                    int offset = 80;
                    int start = 0;

                    boolean first = true;

                    StringBuilder temp = new StringBuilder(100);
                    temp.append(whiteSpace).append("<target>");
                    while (start < value.length()) {
                        if (value.length() > character_offset) {

                            if (!first) {
                                temp.append(whiteSpace).append(" ");
                            }

                            first = false;
                            int end = b.following(offset);
                            String str = value.substring(start, end);
                            start = end;
                            offset += 80;
                            temp.append(str).append(" \\\n");
                        } else {
                            temp.append(value);
                            start = value.length();
                        }
                    }

                    if (value.length() > character_offset) {
                        temp.append(whiteSpace);
                    }

                    temp.append("</target>\n");
                    os.write(temp.toString().getBytes());
                } else {
                    os.write(line.getBytes());
                }

            }
        }
        in.close();
    }
}
