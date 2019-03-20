/*
 * Copyright IBM Corp. 2015, 2018
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
package com.ibm.g11n.pipeline.resfilter.impl;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.BreakIterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

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

import com.ibm.g11n.pipeline.resfilter.FilterOptions;
import com.ibm.g11n.pipeline.resfilter.LanguageBundle;
import com.ibm.g11n.pipeline.resfilter.LanguageBundleBuilder;
import com.ibm.g11n.pipeline.resfilter.ResourceFilter;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterException;
import com.ibm.g11n.pipeline.resfilter.ResourceString;

/**
 * AMD i18n JS resource filter implementation.
 *
 * @author Yoshito Umaoka, Farhan Arshad
 */
public class AmdJsResource extends ResourceFilter {

    private static final String KEY_LINE_PATTERN = "^\\s*\".+\" *:.*";
    private static final String ENTRY_END_LINE_PATTERN = ".*[\\},]\\s*";
    private static final String CLOSE_BRACE_PATTERN = ".*}.*";

    static final int MAX_COLUMNS = 80;

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
    public LanguageBundle parse(InputStream inStream, FilterOptions options)
            throws IOException, ResourceFilterException {
        LanguageBundleBuilder bb = new LanguageBundleBuilder(true);

        // TODO: Rhino parse(Reader, String, int) only throws IOException on IO
        // error thrown
        // by Reader. We need to use ErrorReporter used in the Parser
        // constructor,
        // and check the reporter after parse method to detect JavaScript syntax
        // problems.
        try (InputStreamReader reader = new InputStreamReader(new BomInputStream(inStream), "UTF-8")) {
            AstRoot root = new Parser().parse(reader, null, 1);
            KeyValueVisitor visitor = new KeyValueVisitor();
            root.visitAll(visitor);
            LinkedHashMap<String, String> resultMap = visitor.elements;
            for (Entry<String, String> entry : resultMap.entrySet()) {
                bb.addResourceString(entry.getKey(), entry.getValue());
            }
        }

        return bb.build();
    }

    @Override
    public void write(OutputStream outStream, LanguageBundle languageBundle,
            FilterOptions options) throws IOException, ResourceFilterException {
        try (OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(outStream), "UTF-8")) {
            writer.write("define({\n");
            boolean first = true;
            for (ResourceString res : languageBundle.getSortedResourceStrings()) {
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
    public void merge(InputStream baseStream, OutputStream outStream, LanguageBundle languageBundle,
            FilterOptions options) throws IOException, ResourceFilterException {

        Map<String, String> kvMap = Utils.createKeyValueMap(languageBundle.getResourceStrings());
        BreakIterator brkItr = Utils.getWordBreakIterator(options);

        // do not close the scanner because it closes the base input stream
        @SuppressWarnings("resource")
        Scanner in = new Scanner(baseStream, "UTF-8");
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

                if (kvMap.containsKey(key)) {
                    String value = kvMap.get(key);
                    String baseIndent = extractTabPrefix(left);
                    String indent = getTabStr(baseIndent);

                    pw.write(formatEntry(key, value,'"', MAX_COLUMNS, baseIndent, indent, brkItr));

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
     * Format a pair of resource key/value into the format:
     *     "key1" : "val1"
     * 
     * @param key           The resource key
     * @param value         The resource value
     * @param quote         The String quotation character, usually " or '
     * @param maxColumn     The maximum column to be used. Note, if a single word is longer
     *                      than this value, the line including the word may exceeds this maximum
     *                      column width.
     * @param baseIndent    The base indent, inserted before key
     * @param indent        The indent used for continuation line. The 2nd or later lines will
     *                      start with baseIndent + indent
     * @param brkItr        The break iterator to be used for separating key/value text if necessary
     * @return              A formatted resource key/value string
     */
    static String formatEntry(String key, String value, char quote, int maxColumn, String baseIndent, String indent, BreakIterator brkItr) {
        if (maxColumn < baseIndent.length() + indent.length() + 5 /* quotes and separator */) {
            throw new IllegalArgumentException("Not enough columns to format key/value.");
        }

        final int baseIndentWidth = getSpacesWidth(baseIndent);
        final int indentWidth = getSpacesWidth(indent);

        StringBuilder output = new StringBuilder();
        int remain = maxColumn;

        // Emit base indent
        output.append(baseIndent);
        remain -= baseIndentWidth;

        // Emit opening quote
        output.append(quote);
        remain--;

        int idx = 0;

        // Emit key
        String s = extractText(key, idx, remain - 1 /* space for closing quote */, brkItr);
        idx += s.length();
        output.append(s).append(quote);
        remain -= (s.length() + 1);
        if (idx < key.length()) {
            // Process continuation lines if required
            while (idx < key.length()) {
                output.append('\n').append(baseIndent).append(indent).append("+ ").append(quote);
                remain = maxColumn - baseIndentWidth - indentWidth - 3 /* +<sp><quote> */;
                s = extractText(key, idx, remain - 1, brkItr);
                idx += s.length();
                output.append(s).append(quote);
                remain -= (s.length() + 1);
            }
        }

        // Emit separator
        if (remain < 3 /* <sp>:<sp>*/) {
            // emit separator to next line
            output.append('\n').append(baseIndent).append(indent).append(": ");
            remain = maxColumn - baseIndentWidth - indentWidth - 2 /* :<sp> */;
        } else {
            // emit separator in the same line
            output.append(" : ");
            remain -= 3;
        }

        // Emit first substring from value
        idx = 0;
        s = extractText(value, idx, remain - 2 /* space for opening and closing quotes */, brkItr);
        if (remain < s.length() + 2 /* opening/closing quotes */) {
            // Emit the opening quote to next line
            output.append('\n').append(baseIndent).append(indent).append(quote);

            // Extract value segment again with updated remaining length
            remain = maxColumn - baseIndentWidth - indentWidth - 1 /* opening quote */;
            s = extractText(value, idx, remain - 1 /* space for closing quotes */, brkItr);
            idx += s.length();

            output.append(s).append(quote);
            remain = maxColumn - baseIndentWidth - indentWidth - s.length() - 2;
        } else {
            idx += s.length();
            // Emit the value to the same line
            output.append(quote).append(s).append(quote);
            remain -= (s.length() + 2);
        }

        if (idx < value.length()) {
            // Process continuation lines if required
            while (idx < value.length()) {
                output.append('\n').append(baseIndent).append(indent).append("+ ").append(quote);
                remain = maxColumn - baseIndentWidth - indentWidth - 3 /* +<sp><quote> */;
                s = extractText(value, idx, remain - 1, brkItr);
                idx += s.length();
                output.append(s).append(quote);
                remain -= (s.length() + 1);
            }
        }

        return output.toString();
    }

    /**
     * Extracts a substring that fits within the specified maximum length. When the very
     * first segment of the given text starting with the index exceeds the specified maximum
     * length, this method still returns the segment. So this method always returns a
     * non-empty string.
     * 
     * @param text      The base text
     * @param startIdx  The start index within the text to be processed
     * @param maxLen    The maximum length of substring. Note: this restriction is not
     *                  enforced for the very first text segment.
     * @param brkItr    The break iterator to be used for segmenting text.
     * @return          A substring
     */
    static String extractText(String text, int startIdx, int maxLen, BreakIterator brkItr) {
        String s = text.substring(startIdx);
        brkItr.setText(s);
        int idx = brkItr.next();
        while (true) {
            int tmp = brkItr.next();
            if (tmp == BreakIterator.DONE || tmp >= maxLen) {
                break;
            }
            idx = tmp;
        }
        return s.substring(0, idx);
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
     * Returns the whitespace prefix of the provided string e.g. if s = " hello"
     *
     * Then the return value will be " ".
     */
    private static String extractTabPrefix(String s) {
        int i = 0;
        while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) {
            i++;
        }

        return s.substring(0, i);
    }


    private static final int TAB_WIDTH = 4;
    /**
     * Gets the display width of the whitespace string is using. Tab chars are
     * equal to TAB_WIDTH. i.e. a tab is considered to be of 4 in width.
     */
    static int getSpacesWidth(String whitespace) {
        int size = 0;
        for (int i = 0; i < whitespace.length(); i++) {
            if (whitespace.charAt(i) == '\t') {
                size += TAB_WIDTH;
            } else if (whitespace.charAt(i) == ' ') {
                size++;
            }
        }
        return size;
    }
}
