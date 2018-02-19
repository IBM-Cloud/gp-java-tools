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
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.BreakIterator;
import java.util.List;

import com.ibm.g11n.pipeline.resfilter.FilterOptions;
import com.ibm.g11n.pipeline.resfilter.LanguageBundle;
import com.ibm.g11n.pipeline.resfilter.LanguageBundleBuilder;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterException;
import com.ibm.g11n.pipeline.resfilter.ResourceString;

/**
 * Resource filter for GetText PO files. Supports reading of PO files to extract
 * the msgid and msgstr values, and writing of provided entries to PO files.
 *
 * @author Farhan Arshad
 */
public class POResource extends POTResource {

    @Override
    public LanguageBundle parse(InputStream inStream, FilterOptions options)
            throws IOException, ResourceFilterException {

        LanguageBundleBuilder bb = new LanguageBundleBuilder(true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, CHAR_SET));

        // use these to store the state of an entry
        String singularKey = null;
        String pluralKey = null;
        boolean singularValueSet = false;

        String line;
        while ((line = reader.readLine()) != null) {

            if (line.isEmpty()) {
                // reset state, new entry is starting next
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
            } else if (singularKey != null && pluralKey == null && line.startsWith(UNTRANSLATED_PLURAL_STRING_PREFIX)) {
                // save the plural key for next loop iteration
                pluralKey = value;
            } else if (singularKey != null && pluralKey == null && line.startsWith(TRANSLATED_STRING_PREFIX)
                    && !line.startsWith(TRANSLATED_PLURAL_0_STRING_PREFIX)) {
                // this covers the normal case when:
                // msgid "untranslated-string"
                // msgstr "translated-string"
                
                bb.addResourceString(singularKey, value);
            } else if (singularKey != null && pluralKey != null && line.startsWith(TRANSLATED_PLURAL_0_STRING_PREFIX)) {
                // this covers the singular key/value in a plural entry
                // the key is the value of msgid and the value is that of
                // msgstr[0]
                // msgid "Unable to find user: @users"
                // msgid_plural "Unable to find users: @users"
                // msgstr[0] "Benutzer konnte nicht gefunden werden: @users"
                // msgstr[1] "Benutzer konnten nicht gefunden werden: @users"

                bb.addResourceString(singularKey, value);
                singularValueSet = true;
            } else if (singularKey != null && pluralKey != null && singularValueSet
                    && line.startsWith(TRANSLATED_PLURAL_1_STRING_PREFIX)) {
                // this covers the plural key/value in a plural entry
                // the key is the value of msgid_plural and the value is that of
                // msgstr[1]
                bb.addResourceString(pluralKey, value);
            }
        }

        return bb.build();
    }

    @Override
    public void write(OutputStream outStream, LanguageBundle languageBundle,
            FilterOptions options) throws IOException, ResourceFilterException {

        List<ResourceString> resStrings = languageBundle.getSortedResourceStrings();

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream, CHAR_SET));

        BreakIterator brkItr = Utils.getWordBreakIterator(options);

        // write header
        writer.write(getHeader());

        // write entries
        for (ResourceString res : resStrings) {
            writer.newLine();
            writer.write(formatMessage(UNTRANSLATED_STRING_PREFIX, res.getKey(), brkItr));
            writer.write(formatMessage(TRANSLATED_STRING_PREFIX, res.getValue(), brkItr));
        }

        writer.flush();
    }
}
