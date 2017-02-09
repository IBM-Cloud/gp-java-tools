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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

        // Upload resource data for source language and target languages
        Set<String> remainingLangs = targetLangs == null ?
                Collections.<String>emptySet() : new HashSet<>(targetLangs);

        Map<String, ResourceEntryData> srcResources = srcClient.getResourceEntries(
                srcBundleId, srcLang);

        if (!srcResources.isEmpty()) {
            uploadResources(destClient, destBundleId, srcLang, srcResources);

            // Upload resource data for target languages
            // Removing already processed target languages from remainingLangs
            // while iterating through the set, so we need to use Iterator here.
            for (Iterator<String> i = remainingLangs.iterator(); i.hasNext();) {
                String trgLang = i.next();
                Map<String, ResourceEntryData> resources = srcClient.getResourceEntries(srcBundleId, trgLang);
                if (!resources.isEmpty()) {
                    if (uploadResources(destClient, destBundleId, trgLang, resources)) {
                        // if something has been uploaded, the language was automatically
                        // added to the bundle's target language list. So remove the language
                        // from the 'remaining' language list.
                        i.remove();
                    }
                }
            }
        }

        if (!remainingLangs.isEmpty()) {
            // There are missing target languages.
            // This happens either when the source bundle is empty,
            // or target bundles does not contain any successful translated
            // contents. We want to set the set of target languages
            // to the destination bundle.
            assert targetLangs != null;
            BundleDataChangeSet bundleDataChanges = new BundleDataChangeSet();
            bundleDataChanges.setTargetLanguages(targetLangs);
            destClient.updateBundle(destBundleId, bundleDataChanges);
        }
    }

    /**
     * Upload resource entries. An entry for a target language with status
     * other than 'translated' will be excluded.
     * 
     * @param client    GP service client
     * @param bundleId  Bundle ID
     * @param language  Language ID
     * @param resources Resource entries, originally read from another bundle
     * @return  true if this method actually uploaded any resource entries, or false
     *          if nothing uploaded.
     * @throws ServiceException
     */
    private static boolean uploadResources(ServiceClient client, String bundleId,
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

        if (newResources.isEmpty()) {
            return false;
        }
        client.uploadResourceEntries(bundleId, language, newResources);
        return true;
    }
}