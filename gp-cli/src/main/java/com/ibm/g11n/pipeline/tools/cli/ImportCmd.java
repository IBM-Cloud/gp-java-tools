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
package com.ibm.g11n.pipeline.tools.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.ibm.g11n.pipeline.client.NewResourceEntryData;
import com.ibm.g11n.pipeline.client.ServiceException;
import com.ibm.g11n.pipeline.resfilter.FilterOptions;
import com.ibm.g11n.pipeline.resfilter.LanguageBundle;
import com.ibm.g11n.pipeline.resfilter.ResourceFilter;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterException;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterFactory;
import com.ibm.g11n.pipeline.resfilter.ResourceString;

/**
 * Imports resource data to a translate bundle.
 * 
 * @author Yoshito Umaoka
 */
@Parameters(commandDescription = "Imports resource data to a translate bundle.")
final class ImportCmd extends BundleCmd {
    @Parameter(
            names = {"-l", "--language"},
            description = "Language ID",
            required = true)
    private String languageId;

    @Parameter(
            names = {"-t", "-type"},
            description = "Resource file type",
            required = true)
    private String type;

    @Parameter(
            names = {"-f", "--file"},
            description = "File name to be imported",
            required = true)
    private String fileName;

    @Parameter(
            names = {"-r", "--reviewed"},
            description = "Mark imported resource strings as reviewed")
    private boolean asReviewed = false;

    @Override
    protected void _execute() {
        // TODO: Bundle comments should be imported if the language of the resource files is
        // the bundle's source language.

        Map<String, NewResourceEntryData> resEntries = null;
        ResourceFilter filter = ResourceFilterFactory.getResourceFilter(type);
        if (filter == null) {
            throw new RuntimeException("Resource filter for " + type + " is not available.");
        }
        File f = new File(fileName);
        try (FileInputStream fis = new FileInputStream(f)) {
            LanguageBundle bundle = filter.parse(fis, new FilterOptions(Locale.forLanguageTag(languageId)));

            resEntries = new HashMap<>(bundle.getResourceStrings().size());
            for (ResourceString resString : bundle.getResourceStrings()) {
                NewResourceEntryData resEntryData = new NewResourceEntryData(resString.getValue());
                int seqNum = resString.getSequenceNumber();
                if (seqNum >= 0) {
                    resEntryData.setSequenceNumber(Integer.valueOf(seqNum));
                }
                resEntryData.setNotes(resString.getNotes());
                if (asReviewed) {
                    resEntryData.setReviewed(Boolean.TRUE);
                }
                resEntries.put(resString.getKey(), resEntryData);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read the resoruce data from "
                    + fileName + ": " + e.getMessage(), e);
        } catch (ResourceFilterException e) {
            throw new RuntimeException("Failed to parse the resource data in "
                    + fileName + ": " + e.getMessage(), e);
        }

        try {
            getClient().uploadResourceEntries(bundleId, languageId, resEntries);
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Resource data extracted from " + fileName
                + " was successfully imported to bundle:" + bundleId
                + ", language:" + languageId);
    }
}
