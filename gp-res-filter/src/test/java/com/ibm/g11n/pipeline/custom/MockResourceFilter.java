/*  
 * Copyright IBM Corp. 2018
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
package com.ibm.g11n.pipeline.custom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import com.ibm.g11n.pipeline.resfilter.FilterInfo.Type;
import com.ibm.g11n.pipeline.resfilter.FilterOptions;
import com.ibm.g11n.pipeline.resfilter.LanguageBundle;
import com.ibm.g11n.pipeline.resfilter.LanguageBundleBuilder;
import com.ibm.g11n.pipeline.resfilter.ResourceFilter;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterException;
import com.ibm.g11n.pipeline.resfilter.ResourceString;

/**
 * Resource filter implementation for testing.
 * 
 * @author Yoshito Umaoka
 */
public class MockResourceFilter extends ResourceFilter {

    public static final String ID = "MOCK";
    public static final Type TYPE = Type.SINGLE;

    // MockResource format
    //
    // Header lines
    //
    // ! lang
    // # bundle_comment
    // @ metadata_key = meadata_value
    // ---
    //
    // Resource entry lines
    //
    // # bundle_comment1
    // # bundle_comment2
    // @ metadata_key1 = medata_value1
    // @ metadata_key2 = medata_value2
    // resource_key = resource_value
    //

    private static char LANGCODE_MARKER = '!';
    private static char COMMENT_MARKER = '#';
    private static char METADATA_MARKER = '@';
    private static char KEY_VALUE_SEPARATOR_CHAR = '=';
    private static String HEADER_SEPARATOR = "---";

    @Override
    public LanguageBundle parse(InputStream inStream, FilterOptions options)
            throws IOException, ResourceFilterException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8));

        LanguageBundleBuilder bb = new LanguageBundleBuilder(true);
        boolean inHeader = true;
        List<String> notes = null;
        Map<String, String> metadata = null;

        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }

            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            int idx;
            if (inHeader) {
                if (line.charAt(0) == LANGCODE_MARKER) {
                    // language code
                    String langCode = line.substring(1).trim();
                    bb.embeddedLanguageCode(langCode);
                } else if (line.charAt(0) == COMMENT_MARKER) {
                    // bundle comment
                    bb.addNote(line.substring(1).trim());
                } else if (line.charAt(0) == METADATA_MARKER) {
                    // bundle metadata
                    String kv = line.substring(1);
                    idx = kv.indexOf(KEY_VALUE_SEPARATOR_CHAR);
                    if (idx <= 0) {
                        continue;
                    }

                    String metaKey = kv.substring(0, idx).trim();
                    if (metaKey.isEmpty()) {
                        continue;
                    }
                    String metaVal = kv.substring(idx + 1).trim();
                    bb.addMetadata(metaKey, metaVal);
                } else if (line.equals(HEADER_SEPARATOR)) {
                    inHeader = false;
                }
            } else {
                if (line.charAt(0) == COMMENT_MARKER) {
                    if (notes == null) {
                        notes = new LinkedList<>();
                    }
                    notes.add(line.substring(1).trim());
                } else if (line.charAt(0) == METADATA_MARKER) {
                    String kv = line.substring(1);
                    idx = kv.indexOf(KEY_VALUE_SEPARATOR_CHAR);
                    if (idx <= 0) {
                        continue;
                    }

                    String metaKey = kv.substring(0, idx).trim();
                    if (metaKey.isEmpty()) {
                        continue;
                    }
                    String metaVal = kv.substring(idx + 1).trim();
                    if (metadata == null) {
                        metadata = new HashMap<>();
                    }
                    metadata.put(metaKey, metaVal);
                } else {
                    idx = line.indexOf(KEY_VALUE_SEPARATOR_CHAR);
                    if (idx <= 0) {
                        continue;
                    }

                    String resKey = line.substring(0, idx).trim();
                    String resVal = line.substring(idx + 1).trim();

                    bb.addResourceString(
                            ResourceString
                                .with(resKey, resVal)
                                .notes(notes)
                                .metadata(metadata));

                    // reset notes/metadata
                    notes = null;
                    metadata = null;
                }
            }
        }

        return bb.build();
    }

    @Override
    public void write(OutputStream outStream, LanguageBundle languageBundle, FilterOptions options)
            throws IOException, ResourceFilterException {

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_8)))) {
            String langCode = languageBundle.getEmbeddedLanguageCode();
            if (langCode != null) {
                StringBuilder langCodeLine = new StringBuilder();
                langCodeLine.append(LANGCODE_MARKER).append(langCode);
                writer.println(langCodeLine);
            }
    
            List<String> bundleNotes = languageBundle.getNotes();
            for (String bundleNote : bundleNotes) {
                StringBuilder noteLine = new StringBuilder();
                noteLine.append(COMMENT_MARKER).append(bundleNote);
                writer.println(noteLine);
            }
    
            Map<String, String> bundleMetadata = languageBundle.getMetadata();
            for (Entry<String, String> bundleMetaKV : bundleMetadata.entrySet()) {
                StringBuilder metadataLine = new StringBuilder();
                metadataLine.append(METADATA_MARKER)
                    .append(bundleMetaKV.getKey()).append(KEY_VALUE_SEPARATOR_CHAR).append(bundleMetaKV.getValue());
                writer.println(metadataLine);
            }
    
            writer.println(HEADER_SEPARATOR);
    
            List<ResourceString> resStrings = languageBundle.getSortedResourceStrings();
            for (ResourceString resString : resStrings) {
                writer.println();
    
                // write resource entry notes
                List<String> notes = resString.getNotes();
                for (String note : notes) {
                    StringBuilder noteLine = new StringBuilder();
                    noteLine.append(COMMENT_MARKER).append(note);
                    writer.println(noteLine);
                }
    
                // write resource entry metadata
                Map<String, String> metadata = resString.getMetadata();
                for (Entry<String, String> metaKV : metadata.entrySet()) {
                    StringBuilder metadataLine = new StringBuilder();
                    metadataLine.append(METADATA_MARKER)
                        .append(metaKV.getKey()).append(KEY_VALUE_SEPARATOR_CHAR).append(metaKV.getValue());
                    writer.println(metadataLine);
                }
    
                // write resoruce entry key and value
                StringBuilder resKV = new StringBuilder();
                resKV.append(resString.getKey()).append(KEY_VALUE_SEPARATOR_CHAR).append(resString.getValue());
                writer.println(resKV);
            }
        }
    }
}
