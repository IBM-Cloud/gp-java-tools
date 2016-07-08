/*  
 * Copyright IBM Corp. 2015,2016
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

import java.util.Date;
import java.util.Set;

import com.beust.jcommander.Parameters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.g11n.pipeline.client.BundleData;
import com.ibm.g11n.pipeline.client.ServiceException;

/**
 * Prints out a translation bundle's information.
 * 
 * @author Yoshito Umaoka
 */
@Parameters(commandDescription = "Prints out a translation bundle's information.")
final class ShowBundleCmd extends BundleCmd {

    // Used for generating output format
    static class BundleDataJson {
        String sourceLanguage;
        Set<String> targetLanguages;
        boolean readOnly;
        String updatedBy;
        Date updatedAt;
    }

    @Override
    protected void _execute() {
        try {
            BundleData bundleData = getClient().getBundleInfo(bundleId);

            BundleDataJson outJson = new BundleDataJson();

            outJson.sourceLanguage = bundleData.getSourceLanguage();
            outJson.targetLanguages = bundleData.getTargetLanguages();
            outJson.readOnly = bundleData.isReadOnly();
            outJson.updatedBy = bundleData.getUpdatedBy();
            outJson.updatedAt = bundleData.getUpdatedAt();

            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                    .setPrettyPrinting()
                    .create();
            String outStr = gson.toJson(outJson);
            System.out.println(outStr);
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }
}
