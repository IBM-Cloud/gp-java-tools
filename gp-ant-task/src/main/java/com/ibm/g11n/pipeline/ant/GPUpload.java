/*  
 * Copyright IBM Corp. 2016
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
package com.ibm.g11n.pipeline.ant;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import com.ibm.g11n.pipeline.client.BundleData;
import com.ibm.g11n.pipeline.client.NewBundleData;
import com.ibm.g11n.pipeline.client.NewResourceEntryData;
import com.ibm.g11n.pipeline.client.ServiceClient;
import com.ibm.g11n.pipeline.client.ServiceException;
import com.ibm.g11n.pipeline.resfilter.Bundle;
import com.ibm.g11n.pipeline.resfilter.ResourceFilter;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterFactory;
import com.ibm.g11n.pipeline.resfilter.ResourceString;

/**
 * Pushes string resource bundle data to an instance of Globalization
 * Pipeline service for translation.
 * 
 * @author Yoshito Umaoka
 */
public class GPUpload extends GPBase {
    /* (non-Javadoc)
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws BuildException {
        getProject().log("Entering GPUploadMojo#execute()", Project.MSG_DEBUG);
        ServiceClient client = getServiceClient();

        try {
            Set<String> bundleIds = client.getBundleIds();
            List<BundleSet> bundleSets = getBundleSets();

            for (BundleSet bundleSet : bundleSets) {
                String srcLang = bundleSet.getSourceLanguage();
                Set<String> tgtLangs = resolveTargetLanguages(bundleSet);
                List<SourceBundleFile> bundleFiles = getSourceBundleFiles(bundleSet);

                // Process each bundle
                for (SourceBundleFile bf : bundleFiles) {
                	getProject().log(bf.getType() + " : " + bf.getBundleId() + " : " + bf.getFile().getAbsolutePath(), Project.MSG_INFO);

                    // Checks if the bundle already exists
                    String bundleId = bf.getBundleId();
                    boolean createNew = false;
                    if (bundleIds.contains(bundleId)) {
                    	getProject().log("Found bundle:" + bundleId, Project.MSG_INFO);
                        // Checks if the source language matches.
                        BundleData bundle = client.getBundleInfo(bundleId);
                        if (!srcLang.equals(bundle.getSourceLanguage())) {
                            throw new BuildException("The source language in bundle:"
                                    + bundleId + "(" + bundle.getSourceLanguage()
                                    + ") does not match the specified language("
                                    + srcLang + ").");
                        }
                    } else {
                    	getProject().log("bundle:" + bundleId + " does not exist, creating a new bundle.", Project.MSG_INFO);
                        createNew = true;
                    }

                    // Parse the resource bundle file
                    ResourceFilter filter = ResourceFilterFactory.get(bf.getType());
                    Map<String, NewResourceEntryData> resEntries = new HashMap<>();

                    try (FileInputStream fis = new FileInputStream(bf.getFile())) {
                        Bundle resBundle = filter.parse(fis);

                        if (createNew) {
                            NewBundleData newBundleData = new NewBundleData(srcLang);
                            // set target languages
                            if (!tgtLangs.isEmpty()) {
                                newBundleData.setTargetLanguages(new TreeSet<String>(tgtLangs));
                            }
                            // set bundle notes
                            newBundleData.setNotes(resBundle.getNotes());
                            client.createBundle(bundleId, newBundleData);
                            getProject().log("Created bundle: " + bundleId, Project.MSG_INFO);
                        }
                        Collection<ResourceString> resStrings = resBundle.getResourceStrings();
                        for (ResourceString resString : resStrings) {
                            NewResourceEntryData resEntryData = new NewResourceEntryData(resString.getValue());
                            int seqNum = resString.getSequenceNumber();
                            if (seqNum >= 0) {
                                resEntryData.setSequenceNumber(Integer.valueOf(seqNum));
                            }
                            // set resource string notes
                            resEntryData.setNotes(resString.getNotes());
                            resEntries.put(resString.getKey(), resEntryData);
                        }
                    } catch (IOException e) {
                        throw new BuildException("Failed to read the resoruce data from "
                                + bf.getFile().getAbsolutePath() + ": " + e.getMessage(), e);
                    }

                    if (resEntries.isEmpty()) {
                    	getProject().log("No resource entries in " + bf.getFile().getAbsolutePath(), Project.MSG_INFO);
                    } else {
                        // Upload the resource entries
                        client.uploadResourceEntries(bundleId, srcLang, resEntries);
                        getProject().log("Uploaded source language(" + srcLang
                                + ") resource entries(" + resEntries.size() + ") to bundle: " + bundleId, Project.MSG_INFO);
                    }
                }
            }
        } catch (ServiceException e) {
            throw new BuildException("Globalization Pipeline service error", e);
        }
    }
}
