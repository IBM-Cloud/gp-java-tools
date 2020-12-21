/*  
 * Copyright IBM Corp. 2017, 2018
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
import java.util.List;
import java.util.Set;

import com.beust.jcommander.Parameters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.g11n.pipeline.client.DocumentData;
import com.ibm.g11n.pipeline.client.ServiceException;

/**
 * Prints out a translatable document's information.
 * 
 * @author John Emmons
 */
@Parameters(commandDescription = "Prints out a translatable document's information.")
final class ShowDocumentCmd extends DocumentCmd {

    // Used for generating output format
    static class DocumentDataJson {
        String sourceLanguage;
        Set<String> targetLanguages;
        List<String> notes;
        boolean readOnly;
        String updatedBy;
        Date updatedAt;
    }

    @Override
    protected void _execute() {
        try {
            DocumentData documentData = getClient().getDocumentInfo(type,documentId);

            DocumentDataJson outJson = new DocumentDataJson();

            outJson.sourceLanguage = documentData.getSourceLanguage();
            outJson.targetLanguages = documentData.getTargetLanguages();
            outJson.notes = documentData.getNotes();
            outJson.readOnly = documentData.isReadOnly();
            outJson.updatedBy = documentData.getUpdatedBy();
            outJson.updatedAt = documentData.getUpdatedAt();

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
