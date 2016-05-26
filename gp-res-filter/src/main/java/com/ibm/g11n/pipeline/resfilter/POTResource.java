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
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.TreeSet;

import com.ibm.g11n.pipeline.resfilter.ResourceString.ResourceStringComparator;

/**
 * Resource filter for GetText POT files. Supports reading of POT
 * files to extract the msgid values and writing of provided entries to POT
 * files.
 *
 * @author Farhan Arshad
 *
 */
public class POTResource implements ResourceFilter {
    static final char QUOTE_CHAR = '"';
    static final char NEWLINE_CHAR = '\n';
    static final char EMPTY_SPACE_CHAR = ' ';

    static final String CHAR_SET = "UTF-8";
    static final String QUOTE_STRING = "\"";
    static final String EMPTY_QUOTES = "\"\"";

    static final String UNTRANSLATED_STRING_PREFIX = "msgid ";
    static final String UNTRANSLATED_PLURAL_STRING_PREFIX =
            "msgid_plural ";
    static final String TRANSLATED_STRING_PREFIX = "msgstr ";
    static final String TRANSLATED_PLURAL_STRING_PREFIX = "msgstr[";
    static final String TRANSLATED_PLURAL_0_STRING_PREFIX = "msgstr[0] ";
    static final String TRANSLATED_PLURAL_1_STRING_PREFIX = "msgstr[1] ";


    static final String PLURAL_KEY_PREFIX = "{n} ";

    // placed at the beginning of po/pot files
    static final String HEADER =
            "# Translations template for PROJECT.\n" +
            "# Copyright (C) %s ORGANIZATION\n" +
            "# This file is distributed under the same license as the " +
                "PROJECT project.\n" +
            "# FIRST AUTHOR <EMAIL@ADDRESS>, %s.\n" +
            "#\n" +
            "#, fuzzy\n" +
            "msgid \"\"\n" +
            "msgstr \"\"\n" +
            "\"Project-Id-Version: PROJECT VERSION\\n\"\n" +
            "\"Report-Msgid-Bugs-To: EMAIL@ADDRESS\\n\"\n" +
            "\"POT-Creation-Date: %s\\n\"\n" +
            "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n" +
            "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n" +
            "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n" +
            "\"MIME-Version: 1.0\\n\"\n" +
            "\"Content-Type: text/plain; charset=%s\\n\"\n" +
            "\"Content-Transfer-Encoding: 8bit\\n\"\n" +
            "\"Generated-By: Globalization Pipeline\\n\"\n";

    @Override
    public Collection<ResourceString> parse(InputStream inStream) throws IOException {
        if (inStream == null) {
            return Collections.emptyList();
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inStream, CHAR_SET));

        Collection<ResourceString> resultCol = new LinkedList<ResourceString>();

        String line, value;
        int sequenceNum = 0;
        while ((line = reader.readLine()) != null) {
            value = extractMessage(line, reader);
            if (value == null || value.isEmpty()) {
                continue;
            }

            if (line.startsWith(UNTRANSLATED_STRING_PREFIX)
                    || line.startsWith(UNTRANSLATED_PLURAL_STRING_PREFIX)) {
                resultCol.add(new ResourceString(value, value, ++sequenceNum));
            }
        }

        return resultCol;
    }

    @Override
    public void write(OutputStream outStream, String language, Collection<ResourceString> data)
            throws IOException {
        if (data == null || outStream == null) {
            return;
        }

        TreeSet<ResourceString> sortedResources = new TreeSet<>(new ResourceStringComparator());
        sortedResources.addAll(data);

        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(outStream, CHAR_SET));
        // write header
        writer.write(getHeader());

        for (ResourceString res: sortedResources) {
            // write entry in format:
            // msgid "untranslated-string"
            // msgstr ""
            writer.newLine();
            writer.write(UNTRANSLATED_STRING_PREFIX + formatMessage(res.getKey()));
            writer.newLine();
            writer.write(TRANSLATED_STRING_PREFIX + EMPTY_QUOTES);
            writer.newLine();
        }

        writer.flush();
    }

    @Override
    public void merge(InputStream base, OutputStream outStream, String language, Collection<ResourceString> data) throws IOException{
        Scanner in = new Scanner(base, CHAR_SET);
        String line = "";
        while(in.hasNextLine()){
            line = in.nextLine() + NEWLINE_CHAR;

            if (line.indexOf(UNTRANSLATED_STRING_PREFIX) == -1) {
                outStream.write(line.getBytes());
            } else {
                String key = line.split(" ")[1].replace("\"", "").replace("\n", "");
                // TODO: Instead of linear search resource key every time,
                // we may build hash map first.
                for (ResourceString res : data) {
                    if (res.getKey().equals(key)) {
                        String keyLine = UNTRANSLATED_STRING_PREFIX + formatMessage(key) + NEWLINE_CHAR;
                        outStream.write(keyLine.getBytes());
                    } else {
                        outStream.write(line.getBytes());
                    }
                }
            }
        }
        in.close();
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
     * @param line first line in the message
     * @param reader used to read additional lines in case the message is on
     * multiple lines
     * @return extracted message, or null if there is no quotes present
     * @throws IOException if there were issues reading from the BufferedReader
     */
    protected static String extractMessage(String line, BufferedReader reader)
            throws IOException {
        String message = extractMsgBetweenQuotes(line);

        if (message == null) {
            return message;
        }

        String extractedMsg = null;

        // put marker before each line read in case the reader needs to be
        // reset b/c an extra line is read
        reader.mark(256);
        while ((line = reader.readLine()) != null
                && line.trim().startsWith(QUOTE_STRING)
                && (extractedMsg = extractMsgBetweenQuotes(line)) != null) {
            message += extractedMsg;
            reader.mark(256);
        }

        if (line != null) {
            reader.reset();
        }

        return message;
    }

    /**
     * Extracts the message between quotes.<br>
     * <br>
     * e.g. given the input "hello, world", the output will be:<br>
     * hello, world<br>
     *
     * @param line the string from which to extract the message.
     * @return the message between quotes, or null if there were any problems
     */
    protected static String extractMsgBetweenQuotes(String line) {
        int start = line.indexOf(QUOTE_CHAR);
        int end = line.lastIndexOf(QUOTE_CHAR);

        if (start == -1 || end == -1 || start == end) {
            return null;
        }

        // skip escaped quote, i.e. \"
        while (line.charAt(end - 1) == '\\') {
            end = line.lastIndexOf(QUOTE_CHAR, end - 1);

            if (end == -1 || start == end) {
                return null;
            }
        }

        return line.substring(start + 1, end);
    }

    /**
     * Formats the input string according to pot/po entry requirements.<br>
     * <br>
     * i.e. puts quotes around the String, and if the String is too long,
     * splits it onto multiple lines. Max line length is 80 chars.<br>
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
    protected static String formatMessage(String message) {
        int maxLineLen = 77;

        int messageLen = message.length();
        if (messageLen < maxLineLen - TRANSLATED_STRING_PREFIX.length()) {
            return QUOTE_CHAR + message + QUOTE_CHAR;
        }

        StringBuilder formattedMessage = new StringBuilder();

        // two quote chars indicate a multi-line message
        formattedMessage.append(QUOTE_CHAR);
        formattedMessage.append(QUOTE_CHAR);

        int end, emptySpaceIndex;
        for (int start = 0; start < messageLen; start = end) {
            formattedMessage.append(NEWLINE_CHAR);
            end = start + maxLineLen >= messageLen ? messageLen
                                                   : start + maxLineLen;

            // make sure not to split words onto multiple lines
            emptySpaceIndex = message.lastIndexOf(EMPTY_SPACE_CHAR, end);
            if (end != messageLen && emptySpaceIndex != -1
                    && emptySpaceIndex + 1 < messageLen) {
                end = emptySpaceIndex + 1;
            }

            formattedMessage.append(QUOTE_CHAR);
            // sanity check
            if (end > messageLen) {
                end = messageLen;
            }
            formattedMessage.append(message.substring(start, end));
            formattedMessage.append(QUOTE_CHAR);
        }

        return formattedMessage.toString();
    }

    /**
     * Prepare the header by inserting the date, time, year, and char set
     * in the appropriate places.
     *
     * @return prepared header, ready to be inserted into PO/POT file
     */
    protected static String getHeader() {
        // prepare and write header
        Date date = new Date();

        String creationDate =
                new SimpleDateFormat("yyyy-MM-dd HH:mmZ").format(date);

        String year =  new SimpleDateFormat("yyyy").format(date);

        return String.format(HEADER, year, year, creationDate,
               CHAR_SET.toLowerCase());
    }
}
