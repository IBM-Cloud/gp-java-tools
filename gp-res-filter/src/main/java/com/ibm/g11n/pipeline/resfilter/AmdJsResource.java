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
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.BreakIterator;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
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
 * @author Yoshito Umaoka, Farhan Arshad
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
            int sequenceNum = 1;
            for (Entry<String, String> entry : resultMap.entrySet()) {
                resultCol.add(new ResourceString(entry.getKey(), entry.getValue(), sequenceNum++));
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

        // do not close the scanner because it closes the base input stream
        @SuppressWarnings("resource")
        Scanner in = new Scanner(base, "UTF-8");
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_8), true);
        String line;
        while (in.hasNextLine()) {
            line = in.nextLine();

            if (!line.matches(KEY_LINE_PATTERN) || line.trim().endsWith("{")) {
                pw.println(line);
            } else {
                String[] parts = line.split(":");
                String left = parts[0];

                String key = left.substring(left.indexOf('"') + 1, left.lastIndexOf('"')).trim();

                if (resMap.containsKey(key)) {
                    String value = resMap.get(key);
                    String tabPrefix = extractTabPrefix(left);
                    pw.write(formatEntry(key, value, tabPrefix, language));

                    while (!line.matches(ENTRY_END_LINE_PATTERN) && !in.hasNext(CLOSE_BRACE_PATTERN)) {
                        line = in.nextLine();
                    }

                    if (!in.hasNext(CLOSE_BRACE_PATTERN)) {
                        pw.write(',');
                    }
                    pw.println();
                } else {
                    pw.println(line);
                    while (!line.matches(ENTRY_END_LINE_PATTERN)) {
                        line = in.nextLine();
                        pw.println(line);
                    }
                }
            }
        }
    }

    /**
     * Formats the value into the format:
     *
     * \<tabPrefix\>"key" : "value",
     *
     * Entry may be split onto multiple lines in the form: "bear 1": "Brown " +
     * "Bear"
     */
    private static String formatEntry(String key, String value, String tabPrefix, String localeStr) {
        int maxLineLen = 80;

        StringBuilder output = new StringBuilder();

        int keyLen = key.length();
        int valueLen = value.length();

        // entry fits on one line
        if (maxLineLen > keyLen + valueLen + tabPrefix.length() + 7) {
            return output.append(tabPrefix).append('"').append(key).append("\" : \"").append(value).append('"')
                    .toString();
        }

        // message needs to be split onto multiple lines
        output.append(tabPrefix);

        // word breaks differ based on the locale
        Locale locale = localeStr == null ? Locale.getDefault() : new Locale(localeStr);

        BreakIterator wordIterator = BreakIterator.getWordInstance(locale);

        // the available char space once we account for the tabbing
        // spaces and other necessary chars such as quotes
        int available = maxLineLen - getSpacesSize(tabPrefix) - 2;

        // the tab prefix for multi-lined entries
        String tabStr = tabPrefix + getTabStr(tabPrefix);

        // actual size of prefix, i.e. tabs count as 4 spaces
        int tabStrSize = getSpacesSize(tabStr);

        // process the key first
        // splitting it into multiple lines if necessary
        wordIterator.setText(key);
        int start = 0;
        int end = wordIterator.first();
        int prevEnd = end;
        boolean firstLine = true;
        while (end != BreakIterator.DONE) {
            prevEnd = end;
            end = wordIterator.next();
            if (end - start > available) {
                output.append('"').append(key.substring(start, prevEnd)).append('"').append('\n').append(tabStr)
                        .append("+ ");
                start = prevEnd;

                // after first line, indent subsequent lines with 4 additional
                // spaces
                if (firstLine) {
                    available = maxLineLen - tabStrSize - 5;
                    firstLine = false;
                }
            } else if (end == keyLen) {
                output.append('"').append(key.substring(start, end)).append("\" : ");
                available = available - 5 - (end - start);
            }
        }

        // process the key first
        // splitting it into multiple lines if necessary
        wordIterator.setText(value);
        start = 0;
        end = wordIterator.first();
        prevEnd = end;
        firstLine = true;
        while (end != BreakIterator.DONE) {
            prevEnd = end;
            end = wordIterator.next();
            if (end - start > available) {
                output.append('"').append(value.substring(start, prevEnd)).append('"').append('\n').append(tabStr)
                        .append("+ ");
                start = prevEnd;

                // after first line, indent subsequent lines with 4 additional
                // spaces
                if (firstLine) {
                    available = maxLineLen - tabStrSize - 5;
                    firstLine = false;
                }
            } else if (end == valueLen) {
                output.append('"').append(value.substring(start, end)).append('"');
            }
        }

        return output.toString();
    }

    /**
     * This method looks at the provided string to determine if a tab char or
     * spaces are being used for tabbing.
     *
     * Defaults to spaces
     */
    static String getTabStr(String str) {
        if (!str.isEmpty() && str.charAt(0) == '\t') {
            return "\t\t";
        } else {
            return "        ";
        }
    }

    /**
     * Returns the whitespace prefix of the provided string e.g. if s =
     * "    hello"
     *
     * Then the return value will be "    ".
     */
    private static String extractTabPrefix(String s) {
        int i = 0;
        while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) {
            i++;
        }

        return s.substring(0, i);
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
}
