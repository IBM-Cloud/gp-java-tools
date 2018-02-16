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
package com.ibm.g11n.pipeline.tools.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.ibm.g11n.pipeline.client.BundleData;
import com.ibm.g11n.pipeline.client.ResourceEntryData;
import com.ibm.g11n.pipeline.client.ServiceException;
import com.ibm.g11n.pipeline.resfilter.FilterOptions;
import com.ibm.g11n.pipeline.resfilter.LanguageBundle;
import com.ibm.g11n.pipeline.resfilter.LanguageBundleBuilder;
import com.ibm.g11n.pipeline.resfilter.ResourceFilter;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterException;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterFactory;
import com.ibm.g11n.pipeline.resfilter.ResourceString;

/**
 * Exports resource data from a translation bundle.
 * 
 * @author Yoshito Umaoka
 */
@Parameters(commandDescription = "Exports resource data from a translation bundle.")
final class ExportCmd extends BundleCmd {
    @Parameter(
            names = {"-l", "--language"},
            description = "Language ID",
            required = true)
    private String languageId;

    @Parameter(
            names = {"-t", "--type"},
            description = "Resource file type",
            required = true)
    private String type;

    @Parameter(
            names = {"-f", "--file"},
            description = "File name to be exported",
            required = true)
    private String fileName;

    @Parameter(
            names = {"-o", "--sourceFile"},
            description = "Source File to compare against",
            required = false)
    private String sourceFileName;

    @Parameter(
            names = {"-k", "--fallback"},
            description = "Whether if source language value is used if translation is missing",
            required = false)
    private boolean fallback = false;

    @Override
    protected void _execute() {
        Map<String, ResourceEntryData> resEntries = null;
        LanguageBundleBuilder bundleBuilder = new LanguageBundleBuilder(false);
        try {
            // For now, just use language ID specified on the command line
            bundleBuilder.embeddedLanguageCode(languageId);

            BundleData bundleData = getClient().getBundleInfo(bundleId);
            List<String> bundleNotes = bundleData.getNotes();
            if (bundleNotes != null) {
                bundleBuilder.addNotes(bundleNotes);
            }

            resEntries =
                    getClient().getResourceEntries(bundleId, languageId);
            for (Entry<String, ResourceEntryData> entry : resEntries.entrySet()) {
                String key = entry.getKey();
                ResourceEntryData data = entry.getValue();
                String resVal = data.getValue();
                Integer seqNum = data.getSequenceNumber();
                String srcVal = data.getSourceValue();
                List<String> notes = data.getNotes();

                if (resVal == null && fallback) {
                    resVal = srcVal;
                }
                if (resVal != null) {
                    ResourceString.Builder resString = ResourceString.with(key, resVal)
                            .sourceValue(srcVal);

                    if (seqNum != null) {
                        resString.sequenceNumber(seqNum.intValue());
                    }
                    if (notes != null) {
                        resString.notes(notes);
                    }
                    bundleBuilder.addResourceString(resString.build());
                }
            }
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
        LanguageBundle bundle = bundleBuilder.build();

        ResourceFilter filter = ResourceFilterFactory.getResourceFilter(type);
        if (filter == null) {
            throw new RuntimeException("Resource filter for " + type + " is not available.");
        }
        File f = new File(fileName);
        FilterOptions fopts = new FilterOptions(Locale.forLanguageTag(languageId));
        try (FileOutputStream fos = new FileOutputStream(f)) {
            if (sourceFileName != null && !sourceFileName.isEmpty()) {
                FileInputStream fis = new FileInputStream(sourceFileName);
                filter.merge(fis, fos, bundle, fopts);
            } else {
                filter.write(fos, bundle, fopts);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write the resoruce data to " + fileName + ": " + e.getMessage(), e);
        } catch (ResourceFilterException e) {
            throw new RuntimeException("Failed to process the resource data for " + fileName + ": " + e.getMessage(), e);
        }

        System.out.println("Resource data exported from bundle:" + bundleId
                + ", language: " + languageId + " was successfully saved to file "
                + fileName);
    }
}
