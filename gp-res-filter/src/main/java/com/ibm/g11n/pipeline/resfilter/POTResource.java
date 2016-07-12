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
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.BreakIterator;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeSet;

import com.ibm.g11n.pipeline.resfilter.ResourceString.ResourceStringComparator;

/**
 * Resource filter for GetText POT files. Supports reading of POT files to
 * extract the msgid values and writing of provided entries to POT files.
 *
 * @author Farhan Arshad
 *
 */
public class POTResource implements ResourceFilter {
    static final String CHAR_SET = "UTF-8";

    static final String ENTRY_PREFIX_PATTERN = "^(msgid|msgstr).*";

    static final String UNTRANSLATED_STRING_PREFIX = "msgid ";
    static final String UNTRANSLATED_PLURAL_STRING_PREFIX = "msgid_plural ";
    static final String TRANSLATED_STRING_PREFIX = "msgstr ";
    static final String TRANSLATED_PLURAL_STRING_PREFIX = "msgstr[";
    static final String TRANSLATED_PLURAL_0_STRING_PREFIX = "msgstr[0] ";
    static final String TRANSLATED_PLURAL_1_STRING_PREFIX = "msgstr[1] ";

    static final String PLURAL_KEY_PREFIX = "{n} ";

    // placed at the beginning of po/pot files
    static final String HEADER = "# Translations template for PROJECT.\n" + "# Copyright (C) %s ORGANIZATION\n"
            + "# This file is distributed under the same license as the " + "PROJECT project.\n"
            + "# FIRST AUTHOR <EMAIL@ADDRESS>, %s.\n" + "#\n" + "#, fuzzy\n" + "msgid \"\"\n" + "msgstr \"\"\n"
            + "\"Project-Id-Version: PROJECT VERSION\\n\"\n" + "\"Report-Msgid-Bugs-To: EMAIL@ADDRESS\\n\"\n"
            + "\"POT-Creation-Date: %s\\n\"\n" + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n" + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n" + "\"Content-Type: text/plain; charset=%s\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n" + "\"Generated-By: Globalization Pipeline\\n\"\n";

    @Override
    public Collection<ResourceString> parse(InputStream inStream) throws IOException {
        if (inStream == null) {
            return Collections.emptyList();
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, CHAR_SET));

        Collection<ResourceString> resultCol = new LinkedList<ResourceString>();

        String line, value;
        int sequenceNum = 0;
        while ((line = reader.readLine()) != null) {
            value = extractMessage(line, reader);
            if (value == null || value.isEmpty()) {
                continue;
            }

            if (line.startsWith(UNTRANSLATED_STRING_PREFIX) || line.startsWith(UNTRANSLATED_PLURAL_STRING_PREFIX)) {
                resultCol.add(new ResourceString(value, value, ++sequenceNum));
            }
        }

        return resultCol;
    }

    @Override
    public void write(OutputStream outStream, String language, Collection<ResourceString> data) throws IOException {
        if (data == null || outStream == null) {
            return;
        }

        TreeSet<ResourceString> sortedResources = new TreeSet<>(new ResourceStringComparator());
        sortedResources.addAll(data);

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream, CHAR_SET));
        // write header
        writer.write(getHeader());

        for (ResourceString res : sortedResources) {
            // write entry in format:
            // msgid "untranslated-string"
            // msgstr ""
            writer.newLine();
            writer.write(formatMessage(UNTRANSLATED_STRING_PREFIX, res.getKey(), language));
            writer.write(TRANSLATED_STRING_PREFIX);
            writer.write("\"\"");
            writer.newLine();
        }

        writer.flush();
    }

    /**
     * Note:
     * {@link POTResource#merge(InputStream, OutputStream, String, Collection)}
     * does not support plural forms in PO/POT files, they will be left as-is.
     */
    @Override
    public void merge(InputStream base, OutputStream outStream, String language, Collection<ResourceString> data)
            throws IOException {
        // put res data into a map for easier searching
        Map<String, String> resMap = new HashMap<String, String>(data.size() * 4 / 3 + 1);
        for (ResourceString res : data) {
            resMap.put(res.getKey(), res.getValue());
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(base, CHAR_SET));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream, CHAR_SET));

        String line;
        while ((line = reader.readLine()) != null) {
            // if the line is a msgid, extract the value and check if the key is
            // in the resMap
            if (line.startsWith(UNTRANSLATED_STRING_PREFIX)) {
                String key = extractMessage(line, reader);

                // write the msgid
                do {
                    writer.write(line);
                    writer.newLine();
                } while ((line = reader.readLine()) != null && line.trim().startsWith("\""));

                // write msgstr
                if (!line.startsWith(TRANSLATED_STRING_PREFIX) || key == null || key.isEmpty()
                        || !resMap.containsKey(key) || resMap.get(key) == null) {
                    // key not found, write msgstr as-is
                    do {
                        writer.write(line);
                        writer.newLine();
                    } while ((line = reader.readLine()) != null && !line.isEmpty());

                    if (line != null && line.isEmpty()) {
                        writer.newLine();
                    }
                } else {
                    // key found, write new msgstr
                    writer.write(formatMessage(TRANSLATED_STRING_PREFIX, resMap.get(key), language));
                    writer.newLine();

                    // skip the old msgstr in the input stream
                    while ((line = reader.readLine()) != null && !line.isEmpty())
                        ;
                }
            } else {
                // write line as-is
                writer.write(line);
                writer.newLine();
            }
        }

        writer.flush();
    }

    /**
     * Extract message between quotes, possibly spanning multiple lines.<br>
     * <br>
     * e.g. if the input is: <br>
     * <br>
     * msgid ""<br>
     * "Hello, this "<br>
     * "is a "<br>
     * "multi-line message."<br>
     * <br>
     * the extracted message will be:<br>
     * <br>
     * Hello, this is a multi-line message.<br>
     *
     * NOTE: This method <b> does not <b> advance the stream marker. Once the
     * method returns, the stream can be read from where it was originally.
     *
     * @param firstLine
     *            first line in the message
     * @param reader
     *            used to read additional lines in case the message is on
     *            multiple lines
     * @return extracted message, or null if there is no quotes present
     * @throws IOException
     *             if there were issues reading from the BufferedReader
     */
    static String extractMessage(String firstLine, BufferedReader reader) throws IOException {
        String extractedMsg = extractMsgBetweenQuotes(firstLine);

        if (extractedMsg == null) {
            return null;
        }

        StringBuilder message = new StringBuilder();
        message.append(extractedMsg);

        // the Scanner will read ahead, the marker must be
        // reset to where it started
        reader.mark(1024);

        // do not close the scanner because it closes the
        // underlying input stream
        @SuppressWarnings("resource")
        Scanner scanner = new Scanner(reader);

        // check if the next line begins with a quote char,
        // this indicates that the value is on multiple lines
        while (scanner.hasNextLine() && scanner.hasNext("\\s*\".*")
                && (extractedMsg = extractMsgBetweenQuotes(scanner.nextLine())) != null) {
            message.append(extractedMsg);
        }

        reader.reset();
        return message.toString();
    }

    /**
     * Extracts the message between quotes.<br>
     * <br>
     * e.g. given the input "hello, world", the output will be:<br>
     * hello, world<br>
     *
     * @param line
     *            the string from which to extract the message.
     * @return the message between quotes, or null if there were any problems
     */
    static String extractMsgBetweenQuotes(String line) {
        int start = line.indexOf('"');
        int end = line.lastIndexOf('"');

        if (start == -1 || end == -1 || start == end) {
            return null;
        }

        // skip escaped quote, i.e. \"
        while (line.charAt(end - 1) == '\\') {
            end = line.lastIndexOf('"', end - 1);

            if (end == -1 || start == end) {
                return null;
            }
        }

        return line.substring(start + 1, end);
    }

    /**
     * Formats the input string according to pot/po entry requirements.<br>
     * <br>
     * i.e. puts quotes around the String, and if the String is too long, splits
     * it onto multiple lines. Max line length is 80 chars.<br>
     * <br>
     * e.g. assuming the input String "Hello, this is a multi-line message" is
     * too long, it will split it into multiple lines:<br>
     * <br>
     * ""<br>
     * "Hello, this "<br>
     * "is a "<br>
     * "multi-line message."<br>
     *
     * @param message
     * @return
     */
    static String formatMessage(String prefix, String message, String localeStr) {
        int maxLineLen = 80;

        int messageLen = message.length();

        StringBuilder output = new StringBuilder(messageLen + (messageLen / maxLineLen) * 2 + prefix.length());

        output.append(prefix);

        // message fits on one line
        if (maxLineLen > messageLen + prefix.length() + 2) {
            return output.append('"').append(message).append("\"\n").toString();
        }

        // message needs to be split onto multiple lines
        output.append("\"\"\n");

        // word breaks differ based on the locale
        Locale locale;
        if (localeStr == null) {
            locale = Locale.getDefault();
        } else {
            locale = new Locale(localeStr);
        }
        BreakIterator wordIterator = BreakIterator.getWordInstance(locale);
        wordIterator.setText(message);

        int available = maxLineLen - 2;

        // a word iterator is used to traverse the message;
        // a reference to the previous word break is kept
        // so that once the current reference goes beyond
        // the available char limit, the message can be split
        // without going over the limit
        int start = 0;
        int end = wordIterator.first();
        int prevEnd = end;
        while (end != BreakIterator.DONE) {
            prevEnd = end;
            end = wordIterator.next();
            if (end - start > available) {
                output.append('"').append(message.substring(start, prevEnd)).append("\"\n");
                start = prevEnd;
            } else if (end == messageLen) {
                output.append('"').append(message.substring(start, end)).append("\"\n");
            }
        }

        return output.toString();
    }

    /**
     * Prepare the header by inserting the date, time, year, and char set in the
     * appropriate places.
     *
     * @return prepared header, ready to be inserted into PO/POT file
     */
    static String getHeader() {
        // prepare and write header
        Date date = new Date();

        String creationDate = new SimpleDateFormat("yyyy-MM-dd HH:mmZ").format(date);

        String year = new SimpleDateFormat("yyyy").format(date);

        return String.format(HEADER, year, year, creationDate, CHAR_SET.toLowerCase());
    }
}
