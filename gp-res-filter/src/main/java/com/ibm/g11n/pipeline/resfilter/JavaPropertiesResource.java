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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Java properties resource filter implementation.
 * 
 * @author Yoshito Umaoka
 */
public class JavaPropertiesResource implements ResourceFilter {
    @Override
    public Map<String, String> parse(InputStream inStream) throws IOException {
        Properties props = new Properties();
        props.load(inStream);
        Map<String, String> resultMap = new HashMap<String, String>(props.size());
        for (String key : props.stringPropertyNames()) {
            resultMap.put(key, props.getProperty(key));
        }
        return resultMap;
    }

    @Override
    public void write(OutputStream outStream, String language, Map<String, String> data) throws IOException {
        Properties props = new Properties();
        for (Entry<String, String> entry : data.entrySet()) {
            props.setProperty(entry.getKey(), entry.getValue());
        }
        props.store(outStream, null);
    }

    private static final String PROPS_ENC = "ISO-8859-1";

    private static class PropDef {
        private String key;
        private String value;
        private PropSeparator separator;

        public enum PropSeparator {
            EQUAL('='),
            COLON(':');

            private char sepChar;

            private PropSeparator(char sepChar) {
                this.sepChar = sepChar;
            }

            public char getCharacter() {
                return sepChar;
            }
        }

        private static final String INDENT = "    ";
        private static final int COLMAX = 80;

        public PropDef(String key, String value, PropSeparator separator) {
            this.key = key;
            this.value = value; // This is raw value in file, and may contain escaped Unicode (e.g. \u00C1)
            this.separator = separator;
        };

        public static PropDef parseLine(String line) {
            // Look for both '=' and ':'. Use the first one as the key-value separator
            PropSeparator sep = PropSeparator.EQUAL;
            int idx = line.indexOf(PropSeparator.EQUAL.getCharacter());
            int idxCol = line.indexOf(PropSeparator.COLON.getCharacter());
            if (idxCol > 0 && (idx < 0 || idxCol < idx)) {
                idx = idxCol;
                sep = PropSeparator.COLON;
            }
            if (idx <= 0) {
                return null;
            }

            PropDef pl = new PropDef(line.substring(0, idx).trim(),
                    line.substring(idx + 1).trim(), sep);
            return pl;
        }

        public String getKey() {
            return key;
        }

        @SuppressWarnings("unused")
        public String getValue() {
            return value;
        }

        public PropSeparator getSeparator() {
            return separator;
        }

        public void print(PrintWriter pw, String language) throws IOException {
            StringBuilder buf = new StringBuilder(100);
            int len = key.length() + value.length() + 3;    /* 3 - length of separator plus two SPs */

            if (len <= COLMAX) {
                // Print this property in a single line
                buf.append(key)
                    .append(' ')
                    .append(separator.getCharacter())
                    .append(' ')
                    .append(escapePropValue(value));
                pw.println(buf.toString());
                return;
            }

            // prints out in multiple lines

            // always prints out key and separator in a single line
            buf.append(key)
                .append(' ')
                .append(separator.getCharacter())
                .append(' ');

            if (buf.length() > COLMAX) {
                buf.append('\\');
                pw.println(buf.toString());

                // clear the buffer and indent
                buf.setLength(0);
                buf.append(INDENT);
            }

            BreakIterator brk = BreakIterator.getWordInstance(Locale.forLanguageTag(language));
            brk.setText(value);

            int start = 0;
            int end = brk.next();
            boolean emitNext = false;
            while (end != BreakIterator.DONE) {
                String segment = value.substring(start, end);
                String escSegment = escapePropValue(segment);
                if (emitNext) {
                    // First character in a continuation line must be
                    // a non-space character. Otherwise, keep appending
                    // segments to the current line.
                    if (!Character.isSpaceChar(escSegment.codePointAt(0))) {
                        // This segment is safe as the first word
                        // of a continuation line.
                        buf.append('\\');
                        pw.println(buf.toString());

                        // clear the buffer and indent
                        buf.setLength(0);
                        buf.append(INDENT);
                        emitNext = false;
                    }
                }
                buf.append(escSegment);
                if (buf.length() > COLMAX) {
                    // defer to emit the line after checking
                    // the next segment.
                    emitNext = true;
                }
                start = end;
                end = brk.next();
            }
            // emit the last line
            if (buf.length() > 0) {
                pw.println(buf.toString());
            }
        }
    }

    private static String escapePropValue(String s) {
        StringBuilder escaped = new StringBuilder();
        StringCharacterIterator itr = new StringCharacterIterator(s);
        for (char c = itr.first(); c != CharacterIterator.DONE; c = itr.next()) {
            if (c == '\\') {
                escaped.append("\\\\");
            } else if (c > 0x7F) {
                escaped.append("\\u")
                    .append(String.format("%04X", (int)c));
            } else {
                escaped.append(c);
            }
        }
        return escaped.toString();
    }

    @Override
    public void merge(InputStream base, OutputStream outStream, String language, Map<String,String> data) throws IOException {
        BufferedReader baseReader = new BufferedReader(new InputStreamReader(base, PROPS_ENC));
        PrintWriter outWriter = new PrintWriter(new OutputStreamWriter(outStream, PROPS_ENC));

        String line = null;
        StringBuilder logicalLineBuf = new StringBuilder();
        List<String> orgLines = new ArrayList<String>(8);   // default size - up to 8 continuous lines
        do {
            // logical line that may define a single property, or empty line
            String logicalLine = null;

            line = baseReader.readLine();

            if (line == null) {
                // End of the file - emit lines not yet processed.
                if (!orgLines.isEmpty()) {
                    logicalLine = logicalLineBuf.toString();
                }
            } else {
                String normLine = line.trim();

                if (orgLines.isEmpty()) {
                    // No continuation marker in the previous line
                    if (normLine.startsWith("#") || normLine.startsWith("!")) {
                        // Comment line - print the original line
                        outWriter.println(line);
                    } else if (normLine.endsWith("\\")) {
                        // Continue to the next line
                        logicalLineBuf.append(normLine, 0, normLine.length() - 1);
                        orgLines.add(line);
                    } else {
                        logicalLine = line;
                    }
                } else {
                    // Continued from the previous line
                    if (normLine.endsWith("\\")) {
                        // continues to the next line
                        logicalLineBuf.append(normLine.substring(0, normLine.length() - 1));
                        orgLines.add(line);    // preserve the original line
                    } else {
                        // terminating the current logical property line
                        logicalLineBuf.append(normLine);
                        orgLines.add(line);
                        logicalLine = logicalLineBuf.toString();
                    }
                }
            }

            if (logicalLine != null) {
                PropDef pd = PropDef.parseLine(logicalLine);
                if (pd != null && data.containsKey(pd.getKey())) {
                    String key = pd.getKey();
                    PropDef modPd = new PropDef(key, data.get(key), pd.getSeparator());
                    modPd.print(outWriter, language);
                } else {
                    if (orgLines.isEmpty()) {
                        // Single line
                        outWriter.println(line);
                    } else {
                        // Multiple lines
                        for (String orgLine : orgLines) {
                            outWriter.println(orgLine);
                        }
                    }
                }
                // Clear continuation data
                orgLines.clear();
                logicalLineBuf.setLength(0);
            }
        } while (line != null);

        outWriter.flush();
    }
}
