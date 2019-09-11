/*
 * Copyright IBM Corp. 2015, 2019
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
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

    /**
     * ValueData stores value and start/end offset within JS source
     */
    private static class ValueData {
        private String value;
        private int start;
        private int end;

        public ValueData(String value, int start, int end) {
            this.value = value;
            this.start = start;
            this.end = end;
        }

        public String getValue() {
            return value;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }
    }

    private static class KeyValueVisitor implements NodeVisitor {
        LinkedHashMap<String, ValueData> elements = new LinkedHashMap<>();

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
                    ValueData val = concatStringNodes(propVal);
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

        private ValueData concatStringNodes(Node node) {
            if (node == null) {
                return null;
            }

            String result = "";
            int start = -1;
            int end = -1;
            boolean lastStringLiteral = true;

            node = removeParenthes(node);
            while (node instanceof InfixExpression) {
                InfixExpression infix = (InfixExpression) node;
                Node left = removeParenthes(infix.getLeft());
                Node right = (infix.getRight());
                if (right instanceof StringLiteral) {
                    String val = ((StringLiteral) right).getValue();
                    result = val + result;
                    if (lastStringLiteral) {
                        end = getNodePosition(right) + getNodeLength(right);
                        lastStringLiteral = false;
                    }
                } else {
                    return null;
                }
                node = left;
            }
            if (node instanceof StringLiteral) {
                String val = ((StringLiteral) node).getValue();
                result = val + result;
                start = getNodePosition(node);
                if (lastStringLiteral) {
                    end = start + getNodeLength(node);
                    lastStringLiteral = false;
                }
            } else {
                return null;
            }
            return new ValueData(result, start, end);
        }

        // Note:
        //
        // Following methods are used for recording location and length of string literal
        // nodes. The parser produces AST and these nodes are instances of AstNode. However,
        // the API used for accessing these objects return Node in the API definition.
        // Therefore, this assumption (a Node here is an instance of AstNode, with position
        // information) could be broken if Rhino's implementation is updated.

        private static int getNodePosition(Node node) {
            if (!(node instanceof AstNode)) {
                throw new RuntimeException("The input node is not an AstNode.");
            }
            return ((AstNode) node).getAbsolutePosition();
        }

        private static int getNodeLength(Node node) {
            if (!(node instanceof AstNode)) {
                throw new RuntimeException("The input node is not an AstNode.");
            }
            return ((AstNode) node).getLength();
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
            LinkedHashMap<String, ValueData> resultMap = visitor.elements;
            for (Entry<String, ValueData> entry : resultMap.entrySet()) {
                bb.addResourceString(entry.getKey(), entry.getValue().getValue());
            }
        }

        return bb.build();
    }

    @Override
    public void write(OutputStream outStream, LanguageBundle languageBundle,
            FilterOptions options) throws IOException, ResourceFilterException {
        try (OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(outStream), StandardCharsets.UTF_8)) {
            writer.write("define({\n");
            boolean first = true;
            final Character quote = '"';
            for (ResourceString res : languageBundle.getSortedResourceStrings()) {
                if (first) {
                    first = false;
                } else {
                    writer.write(",\n");
                }
                writer.write(quote + escapeString(res.getKey(), quote) + quote + ": ");
                writer.write(quote + escapeString(res.getValue(), quote) + quote);
            }
            writer.write("\n});\n");
        }
    }

    @Override
    public void merge(InputStream baseStream, OutputStream outStream, LanguageBundle languageBundle,
            FilterOptions options) throws IOException, ResourceFilterException {

        // Load entire base content to CharSequence
        CharArrayWriter caw = new CharArrayWriter();
        try (InputStreamReader reader = new InputStreamReader(new BomInputStream(baseStream), "UTF-8")) {
            char[] buf = new char[1024];
            int len;
            while ((len = reader.read(buf)) >= 0) {
                caw.write(buf, 0, len);
            }
        }

        char[] baseContent = caw.toCharArray();
        CharArrayReader car = new CharArrayReader(baseContent);

        // Parse base JS and extract key-value data
        AstRoot root = new Parser().parse(car,  null , 1);
        KeyValueVisitor visitor = new KeyValueVisitor();
        root.visitAll(visitor);
        LinkedHashMap<String, ValueData> baseKVMap = visitor.elements;

        // Merge translated value
        Map<String, String> kvMap = Utils.createKeyValueMap(languageBundle.getResourceStrings());

        try (OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(outStream), StandardCharsets.UTF_8)) {
            int idx = 0;    // current index in baseContent
            for (Entry<String, ValueData> baseEntry : baseKVMap.entrySet()) {
                String key = baseEntry.getKey();
                ValueData valData = baseEntry.getValue();
                int start = valData.getStart();
                int end = valData.getEnd();

                if (idx < start) {
                    // write out text up to the start of the original key-value expression
                    writer.write(baseContent, idx, start - idx);
                    idx = start;
                }

                String translatedValue = kvMap.get(key);
                if (translatedValue == null) {
                    // use original value
                    writer.write(baseContent, idx, end - idx);
                } else {
                    // use translated value

                    // opening quote
                    char quote = baseContent[idx];
                    writer.write(quote);

                    // translation value
                    writer.write(escapeString(translatedValue, quote));

                    // closing quote
                    assert quote == baseContent[end - 1];
                    writer.write(quote);
                }
                idx = end;
            }
            if (idx < baseContent.length) {
                writer.write(baseContent, idx, baseContent.length - idx);
            }
        }
    }

    /**
     * Convert Java String object to JavaScript string literal.
     * 
     * @param str       Input String
     * @param quoteChar Character used for JavaScript string literal definition.
     *                  If null, both Quotation mark (U+0022) and Apostrophe (U+0027)
     *                  are escaped.
     * @return  JavaScript string literal expression.
     */
    static String escapeString(String str, Character quoteChar) {
        StringBuilder escaped = null;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            Character esc = null;
            switch (c) {
            case 0x00:  // NUL
                esc = '0';
                break;
            case 0x08:  // BS
                esc = 'b';
                break;
            case 0x09:  // HT
                esc = 't';
                break;
            case 0x0A:  // LF
                esc = 'n';
                break;
            case 0x0B:  // VT 
                esc = 'v';
                break;
            case 0x0C:  // FF
                esc = 'f';
                break;
            case 0x0D:  // CR
                esc = 'r';
                break;
            case 0x22:  // Quotation mark
                if (quoteChar == null || quoteChar.equals('"')) {
                    esc = '"';
                }
                break;
            case 0x27:  // Apostrophe
                if (quoteChar == null || quoteChar.equals('\'')) {
                    esc = '\'';
                }
                break;
            case 0x5C:  // Backslash
                esc = '\\';
                break;
            }

            if (esc != null) {
                if (escaped == null) {
                    // emit characters up to the current index
                    escaped = new StringBuilder(str.subSequence(0, i));
                }
                escaped.append('\\').append(esc);
            } else {
                if (escaped != null) {
                    escaped.append(c);
                }
            }
        }

        return escaped == null ? str : escaped.toString();
    }
}
