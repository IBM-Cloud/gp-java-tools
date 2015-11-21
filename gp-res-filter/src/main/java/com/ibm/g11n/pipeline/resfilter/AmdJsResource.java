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
import java.text.BreakIterator;
import java.util.HashMap;
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

/**
 * AMD i18n JS resource filter implementation.
 * 
 * @author Yoshito Umaoka
 */
public class AmdJsResource implements ResourceFilter {

    private static class KeyValueVisitor implements NodeVisitor {
        Map<String, String> elements = new HashMap<String, String>();

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
    public Map<String, String> parse(InputStream inStream) throws IOException {
        Map<String, String> resultMap = null;
        try (InputStreamReader reader = new InputStreamReader(new BomInputStream(inStream), "UTF-8")) {
            AstRoot root = new Parser().parse(reader, null, 1);
            KeyValueVisitor visitor = new KeyValueVisitor();
            root.visitAll(visitor);
            resultMap = visitor.elements;
        } catch (Exception e) {
            throw new IllegalResourceFormatException(e);
        }
        return resultMap;
    }

    @Override
    public void write(OutputStream outStream, String language, Map<String, String> data) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(outStream), "UTF-8")) {
            writer.write("define({\n");
            boolean first = true;
            for (Entry<String, String> entry : data.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    writer.write(",\n");
                }
                writer.write("\"" + entry.getKey() + "\": " + "\"" + entry.getValue() + "\"");
            }
            writer.write("\n});\n");
        }
    }

    @Override
    public void merge(InputStream base, OutputStream outStream, String language, Map<String, String> data) throws IOException{
        Scanner in = new Scanner(base, "UTF-8");
        
        String line = "";
        
        while(in.hasNextLine()){
            line = in.nextLine() + "\n";
            
            if (line.indexOf(":") == -1){
                outStream.write(line.getBytes());
            }
            
            else {
                String key = line.substring(0, line.indexOf(":")).trim().replace("\"", "");
                final int character_offset = 80;
                
                BreakIterator b = BreakIterator.getWordInstance();
                b.setText(data.get(key));
                
                int offset = 80;
                int start = 0;
                
                boolean first = true;
                
                if (data.containsKey(key)){
                    StringBuilder temp = new StringBuilder(1000);
                    temp.append("\"").append(key).append("\"").append(":");
                    
                    while (start < data.get(key).length()){
                        if (data.get(key).length() > character_offset){
                            if (!first){
                                temp.append(" ");
                            }
                            
                            first = false;
                            int end = b.following(offset);
                            String str = data.get(key).substring(start,end);
                            start = end;
                            offset += 80;
                            temp.append("\"").append(str).append("\"\\\n");
                        }
                        else {
                            temp.append("\"").append(data.get(key)).append("\"");
                            start = data.get(key).length();
                        }
                    }
                    temp.append(",\n");
                    outStream.write(temp.toString().getBytes());
                }
                else {
                    outStream.write(line.getBytes());
                }
            }
        }
        
        in.close();
    }
}
