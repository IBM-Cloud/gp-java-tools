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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import com.ibm.g11n.pipeline.resfilter.ResourceString.ResourceStringComparator;

/**
 *
 * @author Farhan Arshad
 *
 */
public class IOSStringsResource implements ResourceFilter {

    private static final String CHAR_SET = "UTF-8";
    private static final String COMMENT_BEGIN = "/*";
    private static final String COMMENT_END = "*/";
    
    @Override
    public Bundle parse(InputStream in) throws IOException {
        Bundle bundle = new Bundle();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, CHAR_SET));

        int sqNum = 0;
        String line;
        List<String> notes = new ArrayList<>();
        Boolean commentIsGlobal = true;
        while ((line = reader.readLine()) != null) {
            if (line.trim().startsWith(COMMENT_BEGIN)) {// begins with /*
                // skip comments until line ends with */
                // the short circuit expression evaluation handles both
                // single and multi lined comments
                String comment = line.substring(line.indexOf(COMMENT_BEGIN)+2);
                Boolean commentEndProcessed = false;
                while (!commentEndProcessed && line != null) {
                    if (comment.trim().endsWith(COMMENT_END)) {
                        comment = comment.substring(0, comment.lastIndexOf(COMMENT_END));
                        commentEndProcessed = true;
                    }
                    notes.add(comment);
                    if (!commentEndProcessed) {
                        line = reader.readLine();
                        comment = line;
                    }
                }
            } else if (commentIsGlobal && line.isEmpty()) {
                commentIsGlobal = false;
                if (!notes.isEmpty()) {
                    bundle.addNotes(notes);
                    notes.clear();
                }
            } else if (line.matches("^\\s*\".*")) { // begins with quote char
                // new entry
                StringBuilder entry = new StringBuilder(128);

                // keep appending lines until entry termination via semi-colon
                do {
                    entry.append(line.trim()).append(' ');
                } while (!line.matches(".*[^\\\\];\\s*$") && (line = reader.readLine()) != null);

                // split across " = "
                String[] parts = entry.toString().split("\"\\s*=\\s*\"");
                String key = parts[0].substring(parts[0].indexOf('"') + 1).trim();
                String value = parts[1].substring(0, parts[1].lastIndexOf('"')).trim();

                if (notes.isEmpty()) {
                    bundle.addResourceString(key, value, ++sqNum);
                } else {
                    bundle.addResourceString(key, value, ++sqNum, notes);
                    notes.clear();
                }
            }
        }

        return bundle;
    }

    @Override
    public void write(OutputStream os, String language, Bundle bundle) throws IOException {
        TreeSet<ResourceString> sortedResources = new TreeSet<>(new ResourceStringComparator());
        sortedResources.addAll(bundle.getResourceStrings());

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, CHAR_SET));

        boolean globalNotesWritten = false;
        for (String globalNote : bundle.getNotes()) {
            writer.write(COMMENT_BEGIN+globalNote+COMMENT_END);
            writer.newLine();
            globalNotesWritten = true;
        }
        if (globalNotesWritten) {
            writer.newLine();
        }

        for (ResourceString res : sortedResources) {
            // empties the buffer
            writer.write(formatEntry(res.getKey(), res.getValue(), language, res.getNotes()));
        }

        writer.flush();
    }

    @Override
    public void merge(InputStream base, OutputStream os, String language, Bundle bundle)
            throws IOException {
        // put res data into a map for easier searching
        Map<String, ResourceString> resMap = new HashMap<String, ResourceString>(bundle.getResourceStrings().size() * 4 / 3 + 1);
        for (ResourceString res : bundle.getResourceStrings()) {
            resMap.put(res.getKey(), res);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(base, CHAR_SET));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, CHAR_SET));

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.matches("^\\s*\".*")) { // begins with quote char
                // new entry
                StringBuilder entry = new StringBuilder(128);
                List<String> rawLines = new LinkedList<String>();

                // keep appending lines until entry termination via semi-colon
                // also keep a list of lines read so they can be written back
                // if the key is not found
                do {
                    entry.append(line.trim()).append(' ');
                    rawLines.add(line);
                } while (!line.matches(".*[^\\\\];\\s*$") && (line = reader.readLine()) != null);

                // split across " = "
                String[] parts = entry.toString().split("\"\\s*=\\s*\"");
                String key = parts[0].substring(parts[0].indexOf('"') + 1).trim();

                if (!resMap.containsKey(key)) {
                    for (String rawLine : rawLines) {
                        writer.write(rawLine);
                    }
                    writer.newLine();
                    continue;
                }

                writer.write(formatEntry(key, resMap.get(key).getValue(), language, resMap.get(key).getNotes()));
            } else {
                writer.write(line);
                writer.newLine();
            }
        }

        writer.flush();
    }

    static String formatEntry(String key, String value, String localeStr, List<String> notes) {
        int maxLineLen = 80;

        StringBuilder output = new StringBuilder();
        StringBuilder comments = new StringBuilder();

        if (notes.size() > 0) {
            comments.append(COMMENT_BEGIN);
            Iterator<String> i = notes.iterator();
            while (i.hasNext())  {
                comments.append(i.next());
                if (i.hasNext()) {
                    comments.append("\n");
                }
            }
            comments.append(COMMENT_END).append("\n");
        }
        // construct the entire entry, i.e.
        // "key" = "value";
        StringBuilder entryBuilder = new StringBuilder(key.length() + value.length() + 7);
        String entry = entryBuilder.append('"').append(key).append('"').append(" = ").append('"').append(value)
                .append("\";\n").toString();

        int entryLen = entry.length();
        // entry fits on one line
        if (maxLineLen > entryLen) {
            return comments.toString()+entry;
        }

        // entry needs to be split onto multiple lines

        // word breaks differ based on the locale
        Locale locale;
        if (localeStr == null) {
            locale = Locale.getDefault();
        } else {
            locale = new Locale(localeStr);
        }
        BreakIterator wordIterator = BreakIterator.getWordInstance(locale);
        wordIterator.setText(entry);

        int available = maxLineLen;

        // a word iterator is used to traverse the message;
        // a reference to the previous word break is kept
        // so that once the current reference goes beyond
        // the available char limit, the message can be split
        // without going over the limit
        int start = 0;
        int end = wordIterator.first();
        int prevEnd = end;
        boolean firstLine = true;
        while (end != BreakIterator.DONE) {
            prevEnd = end;
            end = wordIterator.next();
            if (end - start > available) {
                output.append(entry.substring(start, prevEnd)).append('\n').append("    ");

                // after first line, indent subsequent lines with 4 spaces
                if (firstLine) {
                    available -= 4;
                    firstLine = false;
                }
                start = prevEnd;
            } else if (end == entryLen) {
                output.append(entry.substring(start, end));
            }
        }

        return comments.toString()+output.toString();
    }

}
