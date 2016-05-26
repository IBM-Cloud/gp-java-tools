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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeSet;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.ParenthesizedExpression;
import org.mozilla.javascript.ast.StringLiteral;

import com.ibm.g11n.pipeline.resfilter.ResourceString.ResourceStringComparator;

/**
 * AMD i18n JS resource filter implementation.
 *
 * @author Yoshito Umaoka
 */
public class AmdJsResource implements ResourceFilter {

    private static String KEY_LINE_PATTERN = "^\\s*\".+\" *:.*";
    private static String ENTRY_END_LINE_PATTERN = ".*[\\},]\\s*";
    private static String CLOSE_BRACE_PATTERN = ".*}.*";

    private static class KeyValueVisitor implements NodeVisitor {
        LinkedHashMap<String, String> elements = new LinkedHashMap<String, String>();

        @Override
        public boolean visit(AstNode node) {
            boolean continueProcessing = true;
            // We only need to check Object Literals
            if (node instanceof ObjectLiteral) {
                List<ObjectProperty> kvProps = null;
                List<ObjectProperty> props = ((ObjectLiteral) node).getElements();
                if (props != null) {
                    // Walk through nodes to check if this is a root bundle with
                    // key/value pairs embedded.
                    for (int i = 0; i < props.size(); i++) {
                        Node left = props.get(i).getLeft();
                        String name = null;
                        if (left instanceof StringLiteral) {
                            name = ((StringLiteral) left).getValue();
                        } else if (left instanceof Name) {
                            name = ((Name) left).getIdentifier();
                        } else {
                            continue;
                        }
                        Node right = props.get(i).getRight();
                        if (name.equalsIgnoreCase("root")) {
                            // This AMD i18n bundle with "root" object
                            // (key/value pairs) embedded.
                            // For example,
                            //
                            // define({
                            // "root": {
                            // "msg.hello": "Hello",
                            // "msg.byte": "Bye"
                            // },
                            // "fr": true,
                            // "de": true
                            // });
                            //
                            right = removeParenthes(right);
                            if (right instanceof ObjectLiteral) {
                                kvProps = ((ObjectLiteral) right).getElements();
                                break;
                            }
                        }
                    }
                }

                if (kvProps == null) {
                    // This bundle contains key/value pairs in the root Object
                    // directly.
                    // For example,
                    //
                    // define({
                    // "msg.hello": "Hello",
                    // "msg.byte": "Bye"
                    // });
                    //
                    kvProps = props;
                }

                // Put key/value pairs to elements
                for (ObjectProperty kv : kvProps) {
                    Node propKey = kv.getLeft();
                    String key = null;
                    if (propKey instanceof Name) {
                        key = ((Name) propKey).getIdentifier();
                    } else if (propKey instanceof StringLiteral) {
                        key = ((StringLiteral) propKey).getValue();
                    }
                    if (key == null) {
                        continue;
                    }

                    Node propVal = kv.getRight();
                    String val = concatStringNodes(propVal);
                    if (val == null) {
                        continue;
                    }
                    elements.put(key, val);
                }
                continueProcessing = false;
            }
            return continueProcessing;
        }

        private Node removeParenthes(Node node) {
            // extract contents from parenthesizes
            // For example,
            //
            // ({a:1})
            // to
            // {a:1}
            //
            if (node != null && node instanceof ParenthesizedExpression) {
                node = ((ParenthesizedExpression) node).getExpression();
            }
            return node;
        }

        private String concatStringNodes(Node node) {
            if (node == null) {
                return null;
            }
            String result = "";
            node = removeParenthes(node);
            while (node instanceof InfixExpression) {
                InfixExpression infix = (InfixExpression) node;
                Node left = removeParenthes(infix.getLeft());
                Node right = (infix.getRight());
                if (right instanceof StringLiteral) {
                    String val = ((StringLiteral) right).getValue();
                    result = val + result;
                } else {
                    return null;
                }
                node = left;
            }
            if (node instanceof StringLiteral) {
                String val = ((StringLiteral) node).getValue();
                result = val + result;
            } else {
                return null;
            }
            return result;
        }
    }

    @Override
    public Collection<ResourceString> parse(InputStream inStream) throws IOException {
        LinkedHashMap<String, String> resultMap = null;
        Collection<ResourceString> resultCol = new LinkedList<ResourceString>();
        try (InputStreamReader reader = new InputStreamReader(new BomInputStream(inStream), "UTF-8")) {
            AstRoot root = new Parser().parse(reader, null, 1);
            KeyValueVisitor visitor = new KeyValueVisitor();
            root.visitAll(visitor);
            resultMap = visitor.elements;
            int sequenceNum = 0;
            for (Entry<String, String> entry : resultMap.entrySet()) {
                sequenceNum++;
                ResourceString res = new ResourceString(entry.getKey(), entry.getValue());
                res.setSequenceNumber(sequenceNum);
                resultCol.add(res);
            }
        } catch (Exception e) {
            throw new IllegalResourceFormatException(e);
        }
        return resultCol;
    }

    @Override
    public void write(OutputStream outStream, String language, Collection<ResourceString> resStrings)
            throws IOException {
        TreeSet<ResourceString> sortedResources = new TreeSet<>(new ResourceStringComparator());
        sortedResources.addAll(resStrings);

        try (OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(outStream), "UTF-8")) {
            writer.write("define({\n");
            boolean first = true;
            for (ResourceString res : sortedResources) {
                if (first) {
                    first = false;
                } else {
                    writer.write(",\n");
                }
                writer.write("\"" + res.getKey() + "\": ");
                writer.write("\"" + res.getValue() + "\"");
            }
            writer.write("\n});\n");
        }
    }

    @Override
    public void merge(InputStream base, OutputStream outStream, String language, Collection<ResourceString> data)
            throws IOException {
        Map<String, String> resMap = new HashMap<String, String>(data.size() * 4 / 3 + 1);
        for (ResourceString res : data) {
            resMap.put(res.getKey(), res.getValue());
        }

        Scanner in = new Scanner(base, "UTF-8");
        String line;
        while (in.hasNextLine()) {
            line = in.nextLine();

            if (!line.matches(KEY_LINE_PATTERN) || line.trim().endsWith("{")) {
                outStream.write(line.getBytes());
                outStream.write('\n');
            } else {
                String[] parts = line.split(":");
                String left = parts[0];

                String key = left.trim().replace("\"", "");

                if (resMap.containsKey(key)) {
                    outStream.write(left.getBytes());
                    outStream.write(": ".getBytes());

                    String value = resMap.get(key);
                    outStream.write(formatValue(value, left).getBytes());

                    while (!line.matches(ENTRY_END_LINE_PATTERN) && !in.hasNext(CLOSE_BRACE_PATTERN)) {
                        line = in.nextLine();
                    }

                    if (!in.hasNext(CLOSE_BRACE_PATTERN)) {
                        outStream.write(',');
                    }
                    outStream.write('\n');
                } else {
                    outStream.write(line.getBytes());
                    outStream.write('\n');
                    while (!line.matches(ENTRY_END_LINE_PATTERN)) {
                        line = in.nextLine();
                        outStream.write(line.getBytes());
                        outStream.write('\n');
                    }
                }
            }
        }
        in.close();
    }

    /**
     * Formats the value into the format:
     *
     * "description" : "Translated - IBM Globalization " +
     *         "Pipeline provides machine translation and editing " +
     *         "capabilities that enable you to rapidly translate your web " +
     *         "or mobile UI and release to your global customers without " +
     *         "having to rebuild or re-deploy your application. Access " +
     *         "Globalization Pipeline capabilities through its dashboard, " +
     *         "RESTful API, or integrate it seamlessly into your " +
     *         "application's Delivery Pipeline. File types such as Java " +
     *         "properties, JSON, AMD i18n are currently supported.",
     *
     * In the example above, formattedKey would be: "description" :
     *
     * @param value
     *            the value to be formatted
     * @param formattedKey
     * @return formatted string
     */
    private static String formatValue(String value, String formattedKey) {
        int initialOffset = formattedKey.length() + 2;

        int spaces = 0;
        for (int i = 0; i < formattedKey.length(); i++) {
            char c = formattedKey.charAt(i);

            if (c == ' ') {
                spaces++;
            } else if (c == '\t') {
                spaces += 4;
            } else {
                break;
            }
        }

        char[] chars = new char[spaces + 8];
        Arrays.fill(chars, ' ');
        String spaceOffSet = new String(chars);

        int extraChar = 6;
        int maxLineLen = 80 - extraChar - spaceOffSet.length();

        int valueLen = value.length();
        if (valueLen < (maxLineLen - initialOffset + 2)) {
            return '"' + value + '"';
        }

        StringBuilder formattedValue = new StringBuilder(valueLen + (valueLen / maxLineLen) * extraChar);

        int end = maxLineLen - initialOffset;
        int start = 0;
        while (start < valueLen) {
            // make sure not to split words onto multiple lines
            int emptySpaceIndex = value.lastIndexOf(" ", end);
            if (end != valueLen && emptySpaceIndex != -1 && emptySpaceIndex + 1 < valueLen) {
                end = emptySpaceIndex + 1;
            }

            formattedValue.append('"');
            // sanity check
            if (end > valueLen) {
                end = valueLen;
            }
            formattedValue.append(value.substring(start, end));

            if (end != valueLen) {
                formattedValue.append("\"\n");
                formattedValue.append(spaceOffSet);
                formattedValue.append("+ ");
            } else {
                formattedValue.append("\"");
            }

            start = end;
            end = start + maxLineLen >= valueLen ? valueLen : start + maxLineLen;
        }

        return formattedValue.toString();
    }
}
