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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.ibm.g11n.pipeline.resfilter.FilterOptions;
import com.ibm.g11n.pipeline.resfilter.LanguageBundle;
import com.ibm.g11n.pipeline.resfilter.LanguageBundleBuilder;
import com.ibm.g11n.pipeline.resfilter.ResourceFilter;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterException;
import com.ibm.g11n.pipeline.resfilter.ResourceString;
import com.ibm.icu.text.MessagePattern;
import com.ibm.icu.text.MessagePattern.Part;
import com.ibm.icu.text.MessagePattern.Part.Type;

/**
 * Java properties resource filter implementation.
 *
 * @author Yoshito Umaoka, JCEmmons
 */
public class JavaPropertiesResource extends ResourceFilter {
    public enum Encoding {
        ISO_8859_1,
        UTF_8;
    }

    public enum MessagePatternEscape {
        /**
         *  Strings with parameter patterns (such as {0}) are processed as message
         *  pattern strings
         */
        AUTO,
        /**
         * All strings are processed as message pattern strings. In other words,
         * literal single quote is escaped on write - e.g. converting "You aren't"
         * to "You aren''t"
         */
        ALL;
    }

    private final Encoding enc;
    private final MessagePatternEscape msgPatEsc;

    public JavaPropertiesResource(Encoding enc, MessagePatternEscape msgPatEsc){
        this.enc = enc;
        this.msgPatEsc = msgPatEsc;
    }
    
    public JavaPropertiesResource(){
        this(Encoding.ISO_8859_1, MessagePatternEscape.AUTO);
    }

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
    public LanguageBundle parse(InputStream inStream, FilterOptions options)
            throws IOException, ResourceFilterException {

        LinkedProperties props = new LinkedProperties();
        BufferedReader inStreamReader = new BufferedReader(new InputStreamReader(inStream, getCharset()));
        String line;
        Map<String, List<String>> notesMap = new HashMap<>();
        List<String> currentNotes = new ArrayList<>();
        boolean globalNotesAvailable = true;
        List<String> globalNotes = null;
        while ((line = inStreamReader.readLine()) != null) {
            line = stripLeadingSpaces(line);
            // Comment line - Add to list of comments (notes) until we find
            // either
            // a blank line (global comment) or a key/value pair
            if (line.startsWith("#") || line.startsWith("!")) {
                // Strip off the leading comment marker, and decode Unicode escape
                // if necessary.
                if (enc == Encoding.UTF_8) {
                    // Do not unescape unicode - because if backslash u encoding is used
                    // in comment, it is on purpose and better not to decode it.
                    currentNotes.add(line.substring(1));
                } else {
                    // Unescape unicode - if not UTF-8 props, backslash u encoding must be
                    // used always.
                    currentNotes.add(unescapeOnlyUnicode(line.substring(1)));
                }
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
                        sb.append(stripLeadingSpaces(continuationLine));
                    }
                }
                String logicalLine = sb.toString();
                PropDef pd = PropDef.parseLine(logicalLine);
                String value = ConvertDoubleSingleQuote(pd.getValue(), msgPatEsc);  
                
                props.setProperty(pd.getKey(), value);
                if (!currentNotes.isEmpty()) {
                    notesMap.put(pd.getKey(), new ArrayList<>(currentNotes));
                    currentNotes.clear();
                }
            }
        }

        Iterator<Object> i = props.orderedKeys().iterator();
        LanguageBundleBuilder bb = new LanguageBundleBuilder(true);
        while (i.hasNext()) {
            String key = (String) i.next();
            List<String> notes = notesMap.get(key);
            bb.addResourceString(ResourceString.with(key, props.getProperty(key)).notes(notes));
        }
        if (globalNotes != null) {
            bb.addNotes(globalNotes);
        }
        return bb.build();
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
    public void write(OutputStream outStream, LanguageBundle languageBundle,
            FilterOptions options) throws IOException, ResourceFilterException {

        List<ResourceString> resStrings = languageBundle.getSortedResourceStrings();
        BreakIterator brkItr = Utils.getWordBreakIterator(options);

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(outStream, getCharset()));
        for (String note : languageBundle.getNotes()) {
            if (enc == Encoding.UTF_8) {
                // No needs to escape raw Unicode charters
                pw.println("#" + note);
            } else {
                // Needs to escape Unicode characters
                pw.println("#" + escapeOnlyUnicode(note));
            }
        }
        if (!languageBundle.getNotes().isEmpty()) {
            pw.println();
        }
        pw.println("#"+new Date().toString());
        for (ResourceString res : resStrings) {
            String value = res.getValue();  
            value = ConvertSingleQuote(value, msgPatEsc);
            PropDef pd = new PropDef(res.getKey(),value,PropDef.PropSeparator.EQUAL,res.getNotes());
            pd.print(pw, brkItr, (enc == Encoding.UTF_8));
        }
        pw.close();
    }

    private Charset getCharset() {
        return enc == Encoding.UTF_8 ? StandardCharsets.UTF_8 : StandardCharsets.ISO_8859_1;
    }

    static class PropDef {
        private String key;
        private String value;
        private PropSeparator separator;
        private List<String> notes;
        
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

        public PropDef(String key, String value, PropSeparator separator, List<String> notes) {
            this.key = key;
            this.value = value;
            this.separator = separator;
            if (notes != null) {
                this.notes = new ArrayList<>();
                this.notes.addAll(notes);
            } else {
                this.notes = null;
            }
        };
        
        public PropDef(String key, String value, PropSeparator separator) {
            this.key = key;
            this.value = value;
            this.separator = separator;
            this.notes = null;
        };

        public static PropDef parseLine(String line) {
            line = stripLeadingSpaces(line);
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
            String value = unescapePropValue(stripLeadingSpaces(line.substring(sepIdx + 1)));
            
            PropDef pl = new PropDef(key, value, sep, null);
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

        public List<String> getNotes() {
            return Collections.unmodifiableList(notes);
        }
        
        public void print(PrintWriter pw, BreakIterator brkItr) throws IOException {
            print(pw, brkItr, false);
        }
        
        public void print(PrintWriter pw, BreakIterator brkItr, boolean isUTF8) throws IOException {
            StringBuilder buf = new StringBuilder(100);
            int len = key.length() + value.length()
                    + 3; /* 3 - length of separator plus two SPs */

            // Write out any notes (comments) associated with this resource.
            if (notes != null) {
                for (String note : notes) {
                    if (isUTF8) {
                        pw.println("#" + note);
                    } else {
                        pw.println("#" + escapeOnlyUnicode(note));
                    }
                }
            }

            if (len <= COLMAX) {
                // Print this property in a single line
                if (separator.getCharacter() == PropSeparator.SPACE.getCharacter()) {
                    buf.append(escapePropKey(key, isUTF8)).append(separator.getCharacter());
                } else {
                    buf.append(escapePropKey(key, isUTF8)).append(' ').append(separator.getCharacter()).append(' ');
                }
                buf.append(escapePropValue(value, isUTF8));
                pw.println(buf.toString());
                return;
            }

            // prints out in multiple lines

            // always prints out key and separator in a single line
            if (separator.getCharacter() == PropSeparator.SPACE.getCharacter()) {
                buf.append(escapePropKey(key, isUTF8)).append(separator.getCharacter());
            } else {
                buf.append(escapePropKey(key, isUTF8)).append(' ').append(separator.getCharacter()).append(' ');
            }

            if (buf.length() > COLMAX) {
                buf.append('\\');
                pw.println(buf.toString());

                // clear the buffer and indent
                buf.setLength(0);
                buf.append(INDENT);
            }

            brkItr.setText(value);

            int start = 0;
            int end = brkItr.next();
            boolean emitNext = false;
            boolean firstSegment = true;
            while (end != BreakIterator.DONE) {
                String segment = value.substring(start, end);
                String escSegment = null;
                if (firstSegment) {
                    escSegment = escape(segment, EscapeSpace.LEADING_ONLY, isUTF8);
                    firstSegment = false;
                } else {
                    escSegment = escape(segment, EscapeSpace.NONE, isUTF8);
                }
                if (emitNext || (buf.length() + escSegment.length() + 2 >= COLMAX)) {
                    // First character in a continuation line must be
                    // a non-space character. Otherwise, keep appending
                    // segments to the current line.
                    if (!isPropsWhiteSpaceChar(escSegment.charAt(0))) {
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
                end = brkItr.next();
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
            builder.append(" Notes=");
            builder.append(getNotes().toString());
            return builder.toString();
        }
    }

    private static final char BACKSLASH = '\\';

    private enum EscapeSpace {
        ALL,
        LEADING_ONLY,
        NONE;
    }

    private static String escape(String str, EscapeSpace escSpace, boolean isUTF8) {
        StringBuilder buf = new StringBuilder();
        int idx = 0;

        // Handle leading space characters
        if (escSpace == EscapeSpace.ALL || escSpace == EscapeSpace.LEADING_ONLY) {
            // Java properties specification considers the characters space (' ', '\u0020'),
            // tab ('\t', '\u0009'), and form feed ('\f', '\u000C') to be white space. 
            // 
            // java.util.Properties#store() implementation escapes space characters
            // to "\ " in key string, as well as leading spaces in value string.
            // Other white space characters are encoded by Unicode escape sequence.
            for (; idx < str.length(); idx++) {
                char c = str.charAt(idx);
                if (c == ' ') {
                    buf.append(BACKSLASH).append(' ');
                } else if (c == '\t') {
                    buf.append(BACKSLASH).append('t');
                } else if (c == '\f') {
                    buf.append(BACKSLASH).append('f');
                } else {
                    break;
                }
            }
        }

        for (int i = idx; i < str.length(); i++) {
            char c = str.charAt(i);

            if (c < 0x20 || c >= 0x7E) {
                // JDK API comment for Properties#store() specifies below:
                //
                // Characters less than \\u0020 and characters greater than \u007E in property keys
                // or values are written as \\uxxxx for the appropriate hexadecimal value xxxx.
                //
                // However, actual implementation uses "\t" for horizontal tab, "\n" for newline
                // and so on. This implementation support the equivalent behavior.
                switch (c) {
                case '\t':
                    buf.append(BACKSLASH).append('t');
                    break;
                case '\n':
                    buf.append(BACKSLASH).append('n');
                    break;
                case '\f':
                    buf.append(BACKSLASH).append('f');
                    break;
                case '\r':
                    buf.append(BACKSLASH).append('r');
                    break;
                default:
                    if(isUTF8){
                       buf.append(c);
                    }else{
                        appendUnicodeEscape(buf, c);
                    }
                    break;
                }
            } else {
                switch (c) {
                case ' ':   // space
                    if (escSpace == EscapeSpace.ALL) {
                        buf.append(BACKSLASH).append(c);
                    } else {
                        buf.append(c);
                    }
                    break;

                // The key and element characters #, !, =, and : are written with
                // a preceding backslash
                case '#':
                case '!':
                case '=':
                case ':':
                case '\\':
                    buf.append(BACKSLASH).append(c);
                    break;

                default:
                    buf.append(c);
                    break;
                }
            }
        }

        return buf.toString();
    }
    
    static String escapePropKey(String str) {
        return escape(str, EscapeSpace.ALL, false);
    }

    static String escapePropKey(String str, boolean isUTF8) {
        return escape(str, EscapeSpace.ALL, isUTF8);
    }
    
    static String escapePropValue(String str) {
        return escape(str, EscapeSpace.LEADING_ONLY, false);
    }

    static String escapePropValue(String str, boolean isUTF8) {
        return escape(str, EscapeSpace.LEADING_ONLY, isUTF8);
    }

    static void appendUnicodeEscape(StringBuilder buf, char codeUnit) {
        buf.append(BACKSLASH).append('u')
        .append(String.format("%04X", (int)codeUnit));
    }

    static String unescapePropKey(String str) {
        return unescape(str);
    }

    static String unescapePropValue(String str) {
        return unescape(str);
    }

    private static String unescape(String str) {
        StringBuilder buf = new StringBuilder();
        boolean isEscSeq = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (isEscSeq) {
                switch (c) {
                case 't':
                    buf.append('\t');
                    break;

                case 'n':
                    buf.append('\n');
                    break;

                case 'f':
                    buf.append('\f');
                    break;

                case 'r':
                    buf.append('\r');
                    break;

                case 'u':
                {
                    // This implementation throws an IllegalArgumentException
                    // when the input string contains a malformed Unicode escape
                    // character sequence. This behavior matches java.util.Properties#load(Reader).
                    final String errMsg = "Malformed \\uxxxx encoding.";
                    if (i + 4 > str.length()) {
                        throw new IllegalArgumentException(errMsg);
                    }
                    // Parse hex digits
                    String hexDigits = str.substring(i + 1, i + 5);
                    try {
                        char codeUnit = (char)Integer.parseInt(hexDigits, 16);
                        buf.append(Character.valueOf(codeUnit));
                        i += 4;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(errMsg, e);
                    }
                    break;
                }

                default:
                    // Special rules applied to Java properties format
                    // beyond standard Java escape character sequence.
                    //
                    // 1. Octal escapes are not recognized
                    // 2. \b does not represent a backspace character
                    // 3. Backslash is dropped from unrecognized escape sequence.
                    //    For example, "\z" is interpreted as a single character 'z'.

                    buf.append(c);
                    break;
                }
                isEscSeq = false;
            } else {
                if (c == BACKSLASH) {
                    isEscSeq = true;
                } else {
                    buf.append(c);
                }
            }
        }

        // Note: Incomplete escape sequence should not be there.
        // This implementation silently drop the character for the case.

        return buf.toString();
    }

    /**
     * Escape non-ASCII code points to backslash u encoded UTF-16 code points. This method
     * does not escape any other control characters and compatible with native2ascii.
     * 
     * @param str An input string
     * @return A string without raw non-ASCII code points.
     */
    private static String escapeOnlyUnicode(String str) {
        final StringBuilder buf = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);
            if (c > 0x7F) {
                appendUnicodeEscape(buf, c);
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    /**
     * Unescape backslash u encoded UTF-16 code points. This method does not
     * unescape any other backslash escape sequence and compatible with native2ascii with
     * -reverse option.
     * 
     * @param str An input string
     * @return A string without backslash u encoded code points.
     */
    private static String unescapeOnlyUnicode(String str) {
        final StringBuilder buf = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);
            if (c == BACKSLASH && i + 5 <= str.length()) {
                boolean isUniEsc = false;
                i++;
                final char c1 = str.charAt(i);
                if (c1 == 'u') {
                    final String hstr = str.substring(i + 1, i + 5);
                    try {
                        final char codeUnit = (char)Integer.parseInt(hstr, 16);
                        buf.append(Character.valueOf(codeUnit));
                        i += 4;
                        isUniEsc = true;
                    } catch (NumberFormatException e) {
                        // Ignore malformed pattern and just emit the sequence
                        // starting backslash 'u'
                    }
                }
                if (!isUniEsc) {
                    // emit a backslash and following character together
                    buf.append(c).append(c1);
                }
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    /***
     * For MessageFormat with number args, convert double single quote back to single quote during import
     * @param inputStr  The message pattern string with single quotes escaped
     * @param msgPatEsc Option for message pattern processing
     * @return  A modified message pattern string not using single quote escape sequences.
     */
    public static String ConvertDoubleSingleQuote(String inputStr, MessagePatternEscape msgPatEsc){
        // Quick check - if there are no doubled single quotes, skip this operation.
        if (inputStr.indexOf("''") < 0) {
            return inputStr;
        }

        MessagePattern msgPat = null;
        try {
            msgPat = new MessagePattern(inputStr);
        } catch (IllegalArgumentException e) {
            // not a message format pattern - fall through
        } catch (IndexOutOfBoundsException e) {
            // might be a valid message format pattern, but cannot handle this - fall through
        }
        if (msgPat == null) {
            // if the string cannot be parsed as a MessageFormat pattern string,
            // just returns the input string.
            return inputStr;
        }

        if (msgPatEsc == MessagePatternEscape.AUTO) {
            // In AUTO mode, checks if the input string contains arguments.
            int numParts = msgPat.countParts();
            boolean hasArguments = false;
            for (int i = 0; i < numParts; i++) {
                Part part = msgPat.getPart(i);
                //Only check ARG_NUMBER at current stage. ARG_NAME may need to be handled in future
                if(part.getType().equals(Type.ARG_NUMBER)){ 
                    hasArguments = true;
                    break;
                }
            }
            if (!hasArguments) {
                // No arguments - just return the string as is
                return inputStr;
            }
        }

        /***
         * '{1}' -> '{1}'
         * '{''}' -> '{''}'
         * '{'' -> '{''
         * '{'}'' -> '{'}'
         * developer''s -> developer's
         */

        StringBuilder outputBuf = new StringBuilder();
        int len = inputStr.length();
        boolean keepQuote = false; //Flag for '{ or '}
        int outstrIndex = 0;
        int quoteIndex = 0;

        while (quoteIndex < len) {
            int idx = inputStr.indexOf("'", quoteIndex);

            if (idx > -1) {
                if (!keepQuote && idx + 1 < len
                        && (inputStr.charAt(idx + 1) == '{' || inputStr.charAt(idx + 1) == '}')) {
                    keepQuote = true;
                    quoteIndex = idx + 2;
                } else {
                    if (keepQuote) { // '{ or '} is not closed yet
                        if (idx + 1 < len && inputStr.charAt(idx + 1) == '\'') { //Ignore ''
                            quoteIndex = idx+2;
                        } else {
                            keepQuote = false; //Close '{ or '} with this '
                            quoteIndex = idx+1;
                        }
                    } else {
                        if (idx + 1 < len && inputStr.charAt(idx + 1) == '\'') { //Convert '' to '
                            outputBuf
                                .append(inputStr.substring(outstrIndex, idx))
                                .append("'");
                            quoteIndex = idx+2;
                            outstrIndex = quoteIndex;
                        } else {
                            // stand alone single quote - just emit it.
                            quoteIndex = idx+1;
                            outputBuf.append(inputStr.substring(outstrIndex, quoteIndex));
                            outstrIndex = quoteIndex;
                        }
                    }
                }
            } else {
                //No single quote is found
                outputBuf.append(inputStr.substring(outstrIndex));
                break;
            }
        }//End while
        
        if (quoteIndex >= len) {
            outputBuf.append(inputStr.substring(outstrIndex));
        }
        
        return outputBuf.toString();
    }

    /***
     * For MessageFormat with number args, convert single quote to double single quote during export
     * @param inputStr  The message pattern without single quotes escaped
     * @param msgPatEsc Option for message pattern processing
     * @return  A modified message pattern string using single quote escape sequences (standard JDK
     *          MessageFormat pattern string).
     */
    public static String ConvertSingleQuote(String inputStr, MessagePatternEscape msgPatEsc){
        // Quick check - if there are no single quotes, skip this operation.
        if (inputStr.indexOf("'") < 0) {
            return inputStr;
        }

        MessagePattern msgPat = null;
        try {
            msgPat = new MessagePattern(inputStr);
        } catch (IllegalArgumentException e) {
            // not a message format pattern - fall through
        } catch (IndexOutOfBoundsException e) {
            // might be a valid message format pattern, but cannot handle this - fall through
        }
        if (msgPat == null) {
            // if the string cannot be parsed as a MessageFormat pattern string,
            // just returns the input string.
            return inputStr;
        }

        if (msgPatEsc == MessagePatternEscape.AUTO) {
            // In AUTO mode, checks if the input string contains arguments.
            int numParts = msgPat.countParts();
            boolean hasArguments = false;
            for (int i = 0; i < numParts; i++) {
                Part part = msgPat.getPart(i);
                //Only check ARG_NUMBER at current stage. ARG_NAME may need to be handled in future
                if(part.getType().equals(Type.ARG_NUMBER)){ 
                    hasArguments = true;
                    break;
                }
            }
            if (!hasArguments) {
                // No arguments - just return the string as is
                return inputStr;
            }
        }


        /***
         * '{1}' -> '{1}'
         * '{''}' -> '{''}'
         * '{'' -> '{''
         * '{'}' -> '{'}''
         * developer's -> developer''s
         */

        StringBuilder outputBuf = new StringBuilder();
        int len = inputStr.length();
        boolean keepQuote = false; //Flag for '{ or '}
        int outstrIndex = 0;
        int quoteIndex = 0;

        while (quoteIndex < len) {
            int idx = inputStr.indexOf("'", quoteIndex);

            if (idx > -1) {
                if (!keepQuote && idx + 1 < len
                        && (inputStr.charAt(idx + 1) == '{' || inputStr.charAt(idx + 1) == '}')) {
                    keepQuote = true;
                    quoteIndex = idx + 2;
                } else {
                    if (keepQuote) { // '{ or '} is not closed yet
                        if (idx + 1 < len && inputStr.charAt(idx + 1) == '\'') { //Ignore ''
                            quoteIndex = idx+2;
                        } else {
                            keepQuote = false; //Close '{ or '} with this '
                            quoteIndex = idx+1;
                        }
                    } else {
                        outputBuf
                            .append(inputStr.substring(outstrIndex, idx))
                            .append("''");
                        quoteIndex = idx+1;
                        outstrIndex = quoteIndex;
                    }
                }
            } else {
                //No single quote is found
                outputBuf.append(inputStr.substring(outstrIndex));
                break;
            }
        }//End while

        if (quoteIndex >= len) {
            outputBuf.append(inputStr.substring(outstrIndex));
        }

        return outputBuf.toString();
    }
    
    public static int findSingleQuote(String inputStr, int start){
        return -1;
    }

    @Override
    public void merge(InputStream baseStream, OutputStream outStream, LanguageBundle languageBundle,
            FilterOptions options) throws IOException, ResourceFilterException {

        Map<String, String> kvMap = Utils.createKeyValueMap(languageBundle.getResourceStrings());

        BufferedReader baseReader = new BufferedReader(new InputStreamReader(baseStream, getCharset()));
        PrintWriter outWriter = new PrintWriter(new OutputStreamWriter(outStream, getCharset()));

        BreakIterator brkItr = Utils.getWordBreakIterator(options);

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
                String normLine = stripLeadingSpaces(line);

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
                if (pd != null && kvMap.containsKey(pd.getKey())) {
                    // Preserve original leading spaces
                    String firstLine = orgLines.isEmpty() ? line : orgLines.get(0);
                    int len = getLeadingSpacesLength(firstLine);
                    if (len > 0) {
                        outWriter.print(firstLine.substring(0, len));
                    }
                    // Write the property key and value
                    String key = pd.getKey();
                    String value = ConvertSingleQuote(kvMap.get(key), msgPatEsc);
                    PropDef modPd = new PropDef(key, value , pd.getSeparator(), null);
                    modPd.print(outWriter, brkItr, (enc == Encoding.UTF_8));
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

    private static int getLeadingSpacesLength(String s) {
        int idx = 0;
        for (; idx < s.length(); idx++) {
            if (!isPropsWhiteSpaceChar(s.charAt(idx))) {
                break;
            }
        }
        return idx;
    }

    private static String stripLeadingSpaces(String s) {
        return s.substring(getLeadingSpacesLength(s));
    }

    private static boolean isPropsWhiteSpaceChar(char c) {
        // Java properties specification considers the characters space (' ', '\u0020'),
        // tab ('\t', '\u0009'), and form feed ('\f', '\u000C') to be white space. 

        return c == ' ' || c == '\t' || c == '\f'; 
    }
}
