/*
 * Copyright IBM Corp. 2015, 2016
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import com.ibm.g11n.pipeline.resfilter.ResourceString.ResourceStringComparator;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Java properties resource filter implementation.
 *
 * @author Yoshito Umaoka
 */
public class JavaPropertiesResource implements ResourceFilter {

    // TODO:
    // This is not a good idea. This implementation might work,
    // but it depends on an assumption that
    // java.util.Properties#load(InputStream)
    // calls Properties#put(Object, Object).

    @SuppressWarnings("serial")
    public class LinkedProperties extends Properties {
        private final HashSet<Object> keys = new LinkedHashSet<Object>();

        public LinkedProperties() {
        }

        public Iterable<Object> orderedKeys() {
            return Collections.list(keys());
        }

        @Override
        public Enumeration<Object> keys() {
            return Collections.<Object> enumeration(keys);
        }

        @Override
        public Object put(Object key, Object value) {
            keys.add(key);
            return super.put(key, value);
        }
    }

    @Override
    public Bundle parse(InputStream inStream) throws IOException {
        LinkedProperties props = new LinkedProperties();
        BufferedReader inStreamReader = new BufferedReader(new InputStreamReader(inStream, PROPS_ENC));
        String line;
        Map<String, List<String>> notesMap = new HashMap<>();
        List<String> currentNotes = new ArrayList<>();
        boolean globalNotesAvailable = true;
        List<String> globalNotes = null;
        while ((line = inStreamReader.readLine()) != null) {
            line = line.trim();
            // Comment line - Add to list of comments (notes) until we find
            // either
            // a blank line (global comment) or a key/value pair
            if (line.startsWith("#") || line.startsWith("!")) {
                // Strip off the leading comment marker, and perform any
                // necessary unescaping here.
                currentNotes.add(StringEscapeUtils.unescapeJava(line.substring(1)));
            } else if (line.isEmpty()) {
                // We are following the convention that the first blank line in
                // a properties
                // file signifies the end of a global comment.
                if (globalNotesAvailable && !currentNotes.isEmpty()) {
                    globalNotes = new ArrayList<>(currentNotes);
                    currentNotes.clear();
                } else {
                    // Just a generic blank line - treat it like a comment.
                    currentNotes.add(line);
                }
                globalNotesAvailable = false;
            } else {
                // Regular non-comment line. If there are notes outstanding that
                // apply
                // to this line, we find its key and add it to the notes map.
                StringBuffer sb = new StringBuffer(line);
                while (isContinuationLine(sb.toString())) {
                    String continuationLine = inStreamReader.readLine();
                    sb.setLength(sb.length() - 1); // Remove the continuation
                                                   // "\"
                    if (continuationLine != null) {
                        sb.append(continuationLine.trim());
                    }
                }
                String logicalLine = sb.toString();
                PropDef pd = PropDef.parseLine(logicalLine);
                props.setProperty(pd.getKey(), pd.getValue());
                if (!currentNotes.isEmpty()) {
                    notesMap.put(pd.getKey(), new ArrayList<>(currentNotes));
                    currentNotes.clear();
                }
            }
        }

        Iterator<Object> i = props.orderedKeys().iterator();
        Bundle result = new Bundle();
        int sequenceNum = 0;
        while (i.hasNext()) {
            String key = (String) i.next();
            List<String> notes = notesMap.get(key);
            ResourceString rs = new ResourceString(key, props.getProperty(key), ++sequenceNum, notes);
            result.addResourceString(rs);
        }
        if (globalNotes != null) {
            result.addNotes(globalNotes);
        }
        return result;
    }

    // This method handles the bizarre edge case where someone might have
    // multiple backslashes at the end of a line.  An even number of them
    // isn't really a continuation, but a backslash in the property value.
    private boolean isContinuationLine(String s) {
        int backslashCount = 0;
        for (int index = s.length() - 1; index >= 0; index--) {
            if (s.charAt(index) != '\\') {
                break;
            }
            backslashCount++;
        }
        return backslashCount % 2 == 1;
    }

    @Override
    public void write(OutputStream outStream, String language, Bundle resource) throws IOException {
        TreeSet<ResourceString> sortedResources = new TreeSet<>(new ResourceStringComparator());
        sortedResources.addAll(resource.getResourceStrings());

        LinkedProperties props = new LinkedProperties();
        for (ResourceString res : sortedResources) {
            props.setProperty(res.getKey(), res.getValue());
        }
        props.store(outStream, null);
    }

    private static final String PROPS_ENC = "ISO-8859-1";

    static class PropDef {
        private String key;
        private String value;
        private PropSeparator separator;

        public enum PropSeparator {
            EQUAL('='), COLON(':'), SPACE(' ');

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
            this.value = value;
            this.separator = separator;
        };

        public static PropDef parseLine(String line) {
            PropSeparator sep = null;
            int sepIdx = -1;

            boolean sawSpace = false;
            for (int i = 0; i < line.length(); i++) {
                char iChar = line.charAt(i);

                if (sawSpace) {
                    if (iChar == PropSeparator.EQUAL.getCharacter()) {
                        sep = PropSeparator.EQUAL;
                    } else if (iChar == PropSeparator.COLON.getCharacter()) {
                        sep = PropSeparator.COLON;
                    } else {
                        sep = PropSeparator.SPACE;
                    }
                } else {
                    if (i > 0 && line.charAt(i - 1) != '\\') {
                        if (iChar == ' ') {
                            sawSpace = true;
                        } else if (iChar == PropSeparator.EQUAL.getCharacter()) {
                            sep = PropSeparator.EQUAL;
                        } else if (iChar == PropSeparator.COLON.getCharacter()) {
                            sep = PropSeparator.COLON;
                        }
                    }
                }

                if (sep != null) {
                    sepIdx = i;
                    break;
                }
            }

            if (sepIdx <= 0 || sep == null) {
                return null;
            }

            String key = unescapePropKey(line.substring(0, sepIdx).trim());
            String value = unescapePropValue(line.substring(sepIdx + 1).trim());

            PropDef pl = new PropDef(key, value, sep);
            return pl;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public PropSeparator getSeparator() {
            return separator;
        }

        public void print(PrintWriter pw, String language) throws IOException {
            StringBuilder buf = new StringBuilder(100);
            int len = key.length() + value.length()
                    + 3; /* 3 - length of separator plus two SPs */

            if (len <= COLMAX) {
                // Print this property in a single line
                if (separator.getCharacter() == PropSeparator.SPACE.getCharacter()) {
                    buf.append(escapePropKey(key)).append(separator.getCharacter());
                } else {
                    buf.append(escapePropKey(key)).append(' ').append(separator.getCharacter()).append(' ');
                }
                buf.append(escapePropValue(value));
                pw.println(buf.toString());
                return;
            }

            // prints out in multiple lines

            // always prints out key and separator in a single line
            if (separator.getCharacter() == PropSeparator.SPACE.getCharacter()) {
                buf.append(escapePropKey(key)).append(separator.getCharacter());
            } else {
                buf.append(escapePropKey(key)).append(' ').append(separator.getCharacter()).append(' ');
            }

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
                if (emitNext || (buf.length() + escSegment.length() + 2 >= COLMAX)) {
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
                if (buf.length() + 2 >= COLMAX) {
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

        @Override
        public boolean equals(Object obj) {
            if (obj.getClass() != PropDef.class)
                return false;
            PropDef p = (PropDef) obj;
            return getKey().equals(p.getKey()) && getValue().equals(p.getValue())
                    && getSeparator().getCharacter() == p.getSeparator().getCharacter();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Key=");
            builder.append(getKey());
            builder.append(" Value=");
            builder.append(getValue());
            builder.append(" Sep=");
            builder.append("'" + getSeparator().getCharacter() + "'");
            return builder.toString();
        }
    }

    private static String escapePropValue(String s) {
        StringBuilder escaped = new StringBuilder();
        StringCharacterIterator itr = new StringCharacterIterator(s);
        for (char c = itr.first(); c != CharacterIterator.DONE; c = itr.next()) {
            if (c == '\\') {
                escaped.append("\\\\");
            } else if (c > 0x7F) {
                escaped.append("\\u").append(String.format("%04X", (int) c));
            } else if (c == ':') {
                escaped.append("\\:");
            } else if (c == '=') {
                escaped.append("\\:");
            } else {
                escaped.append(c);
            }
        }
        return escaped.toString();
    }

    private static String unescapePropValue(String s) {
        StringBuilder unescaped = new StringBuilder();
        StringCharacterIterator itr = new StringCharacterIterator(s);
        for (char c = itr.first(); c != CharacterIterator.DONE; c = itr.next()) {
            if (c == '\\' && itr.getIndex() < itr.getEndIndex()) {
                char n = itr.next();
                if (n == '\\' || n == ':' || n == '=') {
                    unescaped.append(n);
                } else if (n == 'u' && itr.getIndex() + 4 <= itr.getEndIndex()) {
                    StringBuilder unicodeEscape = new StringBuilder("\\u");
                    for (int i = 0; i < 4; i++) {
                        unicodeEscape.append(itr.next());
                    }
                    unescaped.append(StringEscapeUtils.unescapeJava(unicodeEscape.toString()));
                } else {
                    unescaped.append(c);
                    unescaped.append(n);
                }
            } else {
                unescaped.append(c);
            }
        }
        return unescaped.toString();
    }

    private static String escapePropKey(String s) {
        return s.replace(" ", "\\ ");
    }

    private static String unescapePropKey(String s) {
        return s.replaceAll("\\\\ ", " ");
    }

    @Override
    public void merge(InputStream base, OutputStream outStream, String language, Bundle resource) throws IOException {
        Map<String, String> resMap = new HashMap<String, String>(resource.getResourceStrings().size() * 4 / 3 + 1);
        for (ResourceString res : resource.getResourceStrings()) {
            resMap.put(res.getKey(), res.getValue());
        }

        BufferedReader baseReader = new BufferedReader(new InputStreamReader(base, PROPS_ENC));
        PrintWriter outWriter = new PrintWriter(new OutputStreamWriter(outStream, PROPS_ENC));

        String line = null;
        StringBuilder logicalLineBuf = new StringBuilder();
        List<String> orgLines = new ArrayList<String>(8); // default size - up
                                                          // to 8 continuous
                                                          // lines
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
                    } else if (isContinuationLine(normLine)) {
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
                        orgLines.add(line); // preserve the original line
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
                if (pd != null && resMap.containsKey(pd.getKey())) {
                    String key = pd.getKey();
                    PropDef modPd = new PropDef(key, resMap.get(key), pd.getSeparator());
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
