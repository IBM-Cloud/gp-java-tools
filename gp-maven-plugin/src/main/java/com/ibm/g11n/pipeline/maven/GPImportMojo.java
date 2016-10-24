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
package com.ibm.g11n.pipeline.maven;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import com.ibm.g11n.pipeline.client.BundleData;
import com.ibm.g11n.pipeline.client.NewBundleData;
import com.ibm.g11n.pipeline.client.NewResourceEntryData;
import com.ibm.g11n.pipeline.client.ServiceClient;
import com.ibm.g11n.pipeline.client.ServiceException;
import com.ibm.g11n.pipeline.resfilter.ResourceFilter;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterFactory;
import com.ibm.g11n.pipeline.resfilter.ResourceString;

/**
 * GPImportMojo is used for uploading translatable resource bundle
 * contents to a Globalization Pipeline service instance. If a bundle
 * in a service is not available, this implementation creates a new
 * bundle.
 * 
 * @author Yoshito Umaoka
 */
@Mojo(name = "import")
public class GPImportMojo extends GPBaseMojo {
    /* (non-Javadoc)
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().debug("Entering GPImportMojo#execute()");

        List<BundleFile> sourceBundleFiles = getSourceBundleFiles();
        ServiceClient client = getServiceClient();
        String srcLang = getSourceLanguage();
        Set<String> targetLangs = getTargetLanguages();

        try {
            Set<String> bundleIds = client.getBundleIds();

            // Process each bundle
            for (BundleFile bf : sourceBundleFiles) {
                getLog().info(bf.getType() + " : " + bf.getBundleId() + " : " + bf.getFile().getAbsolutePath());

                // Checks if the bundle already exists
                String bundleId = bf.getBundleId();
                if (bundleIds.contains(bundleId)) {
                    getLog().info("Found bundle:" + bundleId);
                    // Checks if the source language matches.
                    BundleData bundle = client.getBundleInfo(bundleId);
                    if (!srcLang.equals(bundle.getSourceLanguage())) {
                        throw new MojoFailureException("The source language in bundle:"
                                + bundleId + "(" + bundle.getSourceLanguage()
                                + ") does not match the specified language("
                                + srcLang + ").");
                    }
                } else {
                    getLog().info("bundle:" + bundleId + " does not exist, creating a new bundle.");
                    NewBundleData newBundleData = new NewBundleData(srcLang);
                    if (!targetLangs.isEmpty()) {
                        newBundleData.setTargetLanguages(new TreeSet<String>(targetLangs));
                    }
                    client.createBundle(bundleId, newBundleData);
                    getLog().info("Created bundle: " + bundleId);
                }

                // Parse the resource bundle file
                ResourceFilter filter = ResourceFilterFactory.get(bf.getType());
                Map<String, NewResourceEntryData> resEntries = new HashMap<>();

                try (FileInputStream fis = new FileInputStream(bf.getFile())) {
                    Collection<ResourceString> resStrings = filter.parse(fis);
                    for (ResourceString resString : resStrings) {
                        NewResourceEntryData resEntryData = new NewResourceEntryData(resString.getValue());
                        int seqNum = resString.getSequenceNumber();
                        if (seqNum >= 0) {
                            resEntryData.setSequenceNumber(Integer.valueOf(seqNum));
                        }
                        resEntries.put(resString.getKey(), resEntryData);
                    }
                } catch (IOException e) {
                    throw new MojoFailureException("Failed to read the resoruce data from "
                            + bf.getFile().getAbsolutePath() + ": " + e.getMessage(), e);
                }

                if (resEntries.isEmpty()) {
                    getLog().info("No resource entries in " + bf.getFile().getAbsolutePath());
                } else {
                    // Upload the resource entries
                    client.uploadResourceEntries(bundleId, srcLang, resEntries);
                    getLog().info("Uploaded source language(" + srcLang
                            + ") resource entries(" + resEntries.size() + ") to bundle: " + bundleId);
                }
            }
        } catch (ServiceException e) {
            throw new MojoFailureException("Globalization Pipeline service error", e);
        }
    }
}
