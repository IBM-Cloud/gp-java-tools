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
package com.ibm.g11n.pipeline.tools.cli;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.ibm.g11n.pipeline.client.BundleData;
import com.ibm.g11n.pipeline.client.BundleDataChangeSet;
import com.ibm.g11n.pipeline.client.NewBundleData;
import com.ibm.g11n.pipeline.client.NewResourceEntryData;
import com.ibm.g11n.pipeline.client.ResourceEntryData;
import com.ibm.g11n.pipeline.client.ServiceAccount;
import com.ibm.g11n.pipeline.client.ServiceClient;
import com.ibm.g11n.pipeline.client.ServiceException;
import com.ibm.g11n.pipeline.client.TranslationStatus;

/**
 * Copies a bundle.
 * 
 * @author Yoshito Umaoka
 */
@Parameters(commandDescription = "Copies a bundle")
final class CopyBundleCmd extends BundleCmd {
    @Parameter(
            names = { "--dest-url"},
            description = "The destinaion service instance's URL")
    private String destUrl;

    @Parameter(
            names = { "--dest-instance-id"},
            description = "The destination service instance ID")
    private String destInstanceId;

    @Parameter(
            names = { "--dest-user-id"},
            description = "User ID used for the destination service instance")
    private String destUserId;

    @Parameter(
            names = { "--dest-password"},
            description = "Password used for the destination service instance")
    private String destPassword;

    @Parameter(
            names = {"-d", "--dest-bundle-id"},
            description = "The destination bundle ID",
            required = true)
    private String destBundleId;


    @Override
    protected void _execute() {
        try {
            ServiceClient srcClient = getClient();
            ServiceClient destClient = null;

            if (destUrl == null && destInstanceId == null && destPassword == null && destUserId == null) {
                destClient = srcClient;
            } else {
                ServiceAccount account = ServiceAccount.getInstance(destUrl, destInstanceId,
                        destUserId, destPassword);
                destClient = ServiceClient.getInstance(account);
            }

            copyBundle(srcClient, bundleId, destClient, destBundleId);
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Bundle:" + bundleId
                + " was successfull copied to the specified destination.");
    }

    static void copyBundle(ServiceClient srcClient, String srcBundleId,
            ServiceClient destClient, String destBundleId) throws ServiceException {

        // First, create the destination bundle without target languages
        BundleData srcBundleInfo = srcClient.getBundleInfo(srcBundleId);
        String srcLang = srcBundleInfo.getSourceLanguage();
        Set<String> targetLangs = srcBundleInfo.getTargetLanguages();

        NewBundleData newBundleData = new NewBundleData(srcLang);
        newBundleData.setMetadata(srcBundleInfo.getMetadata());
        newBundleData.setPartner(srcBundleInfo.getPartner());
        newBundleData.setNoTranslationPattern(srcBundleInfo.getNoTranslationPattern());
        newBundleData.setSegmentSeparatorPattern(srcBundleInfo.getSegmentSeparatorPattern());

        destClient.createBundle(destBundleId, newBundleData);

        // Upload resource data for source language
        Map<String, ResourceEntryData> srcResources = srcClient.getResourceEntries(
                srcBundleId, srcLang);

        Set<String> targetLangsAdded = new HashSet<String>();
        if (!srcResources.isEmpty()) {
            uploadResources(destClient, destBundleId, srcLang, srcResources);

            // Upload resource data for target languages
            if (targetLangs != null && !targetLangs.isEmpty()) {
                for (String lang : targetLangs) {
                    Map<String, ResourceEntryData> resources = srcClient.getResourceEntries(srcBundleId, lang);
                    if (!resources.isEmpty()) {
                        uploadResources(destClient, destBundleId, lang, resources);
                        targetLangsAdded.add(lang);
                    }
                }
            }
        }

        if (!targetLangsAdded.containsAll(targetLangs)) {
            // There are missing target languages.
            // This happens either when the source bundle is empty,
            // or target bundles does not contain any successful translated
            // contents. We want to set the set of target languages
            // to the destination bundle.
            BundleDataChangeSet bundleDataChanges = new BundleDataChangeSet();
            bundleDataChanges.setTargetLanguages(targetLangs);
            destClient.updateBundle(destBundleId, bundleDataChanges);
        }
    }

    private static void uploadResources(ServiceClient client, String bundleId,
            String language, Map<String, ResourceEntryData> resources) throws ServiceException {
        Map<String, NewResourceEntryData> newResources =
                new HashMap<String, NewResourceEntryData>(resources.size());
        for (Entry<String, ResourceEntryData> res : resources.entrySet()) {
            ResourceEntryData resdata = res.getValue();
            TranslationStatus state = resdata.getTranslationStatus();
            if (state != TranslationStatus.SOURCE_LANGUAGE
                    && state != TranslationStatus.TRANSLATED) {
                // Ignore an resource entry, if it's not in successful state
                continue;
            }
            NewResourceEntryData newResData = new NewResourceEntryData(resdata.getValue());
            newResData.setSequenceNumber(resdata.getSequenceNumber());
            newResData.setMetadata(resdata.getMetadata());
            if (resdata.isReviewed()) {
                newResData.setReviewed(Boolean.TRUE);
            }
            newResData.setPartnerStatus(resdata.getPartnerStatus());
            newResources.put(res.getKey(), newResData);
        }

        client.uploadResourceEntries(bundleId, language, newResources);
    }
}