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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

/**
 * Resource filter for GetText PO files. Supports reading of PO files to extract
 * the msgid and msgstr values, and writing of provided entries to PO files.
 *
 * @author Farhan Arshad
 */
public class POResource extends POTResource {

    private static final String TRANSLATED_SINGULAR_STRING_PREFIX = "msgstr[0]";
    private static final String TRANSLATED_PLURAL_STRING_PREFIX = "msgstr[";

    @Override
    public Map<String, String> parse(InputStream inStream) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inStream, CHAR_SET));

        String singularKey = null;
        String pluralKey = null;
        String line, value;
        while ((line = reader.readLine()) != null) {

            if (line.startsWith(UNTRANSLATED_STRING_PREFIX)) {
                value = extractMessage(line, reader);
                if (value != null && !value.isEmpty()) {
                    singularKey = value;
                }
            } else if (line.startsWith(UNTRANSLATED_PLURAL_STRING_PREFIX)) {
                value = extractMessage(line, reader);
                if (value != null && !value.isEmpty()) {
                    pluralKey = value;
                }
            } else if ((line.startsWith(TRANSLATED_STRING_PREFIX)
                    || line.startsWith(TRANSLATED_SINGULAR_STRING_PREFIX))
                    && singularKey != null) {
                value = extractMessage(line, reader);
                if (value != null) {
                    POEntry entry = new POEntry();
                    entry.untranslatedString = singularKey;
                    entry.translatedString = value;
                    map.put(entry.getKeyRepresentation(),
                            entry.translatedString);
                }
            } else if (line.startsWith(TRANSLATED_PLURAL_STRING_PREFIX)
                    && pluralKey != null) {
                value = extractMessage(line, reader);
                if (value != null && !value.isEmpty()) {
                    POEntry entry = new POEntry();
                    int firstBrace = line.indexOf('[');
                    int secondBrace = line.indexOf(']');
                    entry.untranslatedString = line.substring(firstBrace + 1,
                            secondBrace + 1) + pluralKey;
                    entry.translatedString = value;
                    map.put(entry.getKeyRepresentation(),
                            entry.translatedString);
                }
            }
        }

        return map;
    }

    @Override
    public void write(OutputStream outStream, String language, Map<String, String> data)
            throws IOException {
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(outStream, CHAR_SET));

        // write header
        writer.write(getHeader());

        // write entries
        for (Entry<String, String> entry : data.entrySet()) {
            writer.newLine();
            writer.write(
                    UNTRANSLATED_STRING_PREFIX + formatMessage(entry.getKey()));
            writer.newLine();
            writer.write(
                    TRANSLATED_STRING_PREFIX + formatMessage(entry.getValue()));
            writer.newLine();
        }

        writer.flush();
    }

    @Override
    public void merge(InputStream base, OutputStream outStream, String language, Map<String, String> data)
            throws IOException {
        Scanner in = new Scanner(base, CHAR_SET);
        String line = "";

        while (in.hasNextLine()) {
            line = in.nextLine() + NEWLINE_CHAR;

            if (line.indexOf(UNTRANSLATED_STRING_PREFIX) == -1) {
                outStream.write(line.getBytes());
            } else {
                String key = line.split(" ")[1].replace("\"", "").replace("\n", "");
                if (data.containsKey(key)) {
                    String keyLine = UNTRANSLATED_STRING_PREFIX
                            + formatMessage(key) + NEWLINE_CHAR;
                    String valueLine = TRANSLATED_STRING_PREFIX
                            + formatMessage(data.get(key)) + NEWLINE_CHAR;

                    outStream.write(keyLine.getBytes());

                    while((line = in.nextLine()).indexOf(TRANSLATED_STRING_PREFIX) == -1){
                        outStream.write(line.getBytes());
                    }
                    outStream.write(valueLine.getBytes());
                } else {
                    outStream.write(line.getBytes());
                }
            }
        }

        in.close();
    }
}
