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
import java.util.Collection;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.TreeSet;

import com.ibm.g11n.pipeline.resfilter.ResourceString.ResourceStringComparator;

/**
 * Resource filter for GetText PO files. Supports reading of PO files to extract
 * the msgid and msgstr values, and writing of provided entries to PO files.
 *
 * @author Farhan Arshad
 */
public class POResource extends POTResource {

    @Override
    public Collection<ResourceString> parse(InputStream inStream) throws IOException {
        Collection<ResourceString> resStrings = new LinkedList<ResourceString>();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inStream, CHAR_SET));

        String singularKey = null;
        String pluralKey = null;
        String line;
        boolean singularValueSet = false;
        int sequenceNum = 0;
        while ((line = reader.readLine()) != null) {

            if (line.isEmpty()) {
                // reset memory, new entry is starting next
                singularKey = null;
                pluralKey = null;
                singularValueSet = false;
                continue;
            }

            String value = extractMessage(line, reader);
            if (value == null || value.isEmpty()) {
                continue;
            }

            if (line.startsWith(UNTRANSLATED_STRING_PREFIX)) {
             // save the singular key for next loop iteration
                singularKey = value;
            } else if (singularKey != null && pluralKey == null
                    && line.startsWith(UNTRANSLATED_PLURAL_STRING_PREFIX)) {
                // save the plural key for next loop iteration
                pluralKey = value;
            } else if (singularKey != null && pluralKey == null
                    && line.startsWith(TRANSLATED_STRING_PREFIX)
                    && !line.startsWith(TRANSLATED_PLURAL_0_STRING_PREFIX)) {
                // this covers the normal case when:
                // msgid "untranslated-string"
                // msgstr "translated-string"
                resStrings.add(new ResourceString(singularKey, value, ++sequenceNum));
            } else if (singularKey != null && pluralKey != null
                    && line.startsWith(TRANSLATED_PLURAL_0_STRING_PREFIX)) {
                // this covers the singular key/value in a plural entry
                // the key is the value of msgid and the value is that of msgstr[0]
                // msgid "Unable to find user: @users"
                // msgid_plural "Unable to find users: @users"
                // msgstr[0] "Benutzer konnte nicht gefunden werden: @users"
                // msgstr[1] "Benutzer konnten nicht gefunden werden: @users"

                resStrings.add(new ResourceString(singularKey, value, ++sequenceNum));
                singularValueSet = true;
            } else if (singularKey != null && pluralKey != null
                    && singularValueSet
                    && line.startsWith(TRANSLATED_PLURAL_1_STRING_PREFIX)) {
                // this covers the plural key/value in a plural entry
                // the key is the value of msgid_plural and the value is that of msgstr[1]
                resStrings.add(new ResourceString(pluralKey, value, ++sequenceNum));
            }
        }

        return resStrings;
    }

    @Override
    public void write(OutputStream outStream, String language, Collection<ResourceString> data)
            throws IOException {
        TreeSet<ResourceString> sortedResources = new TreeSet<>(new ResourceStringComparator());
        sortedResources.addAll(data);

        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(outStream, CHAR_SET));

        // write header
        writer.write(getHeader());

        // write entries
        for(ResourceString res : sortedResources){
            writer.newLine();
            writer.write(
                    UNTRANSLATED_STRING_PREFIX + formatMessage(res.getKey()));
            writer.newLine();
            writer.write(
                    TRANSLATED_STRING_PREFIX + formatMessage(res.getValue()));
            writer.newLine();
        }

        writer.flush();
    }

    @Override
    public void merge(InputStream base, OutputStream outStream, String language, Collection<ResourceString> data)
            throws IOException {
        Scanner in = new Scanner(base, CHAR_SET);
        String line = "";

        while (in.hasNextLine()) {
            line = in.nextLine() + NEWLINE_CHAR;

            if (line.indexOf(UNTRANSLATED_STRING_PREFIX) == -1) {
                outStream.write(line.getBytes());
            } else {
                String key = line.split(" ")[1].replace("\"", "").replace("\n", "");
                // TODO: Instead of linear search resource key every time,
                // we may create hash map first.
                for (ResourceString res : data) {
                    if (res.getKey().equals(key)) {
                        String keyLine = UNTRANSLATED_STRING_PREFIX
                                + formatMessage(key) + NEWLINE_CHAR;
                        String valueLine = TRANSLATED_STRING_PREFIX
                                + formatMessage(res.getValue()) + NEWLINE_CHAR;

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
        }

        in.close();
    }
}
