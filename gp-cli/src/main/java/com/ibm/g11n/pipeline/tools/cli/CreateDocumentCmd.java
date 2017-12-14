/*  
 * Copyright IBM Corp. 2017
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

import java.util.HashSet;
import java.util.Set;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.ibm.g11n.pipeline.client.NewDocumentData;
import com.ibm.g11n.pipeline.client.ServiceException;

/**
 * Creates a new document for translation.
 * 
 * @author John Emmons
 */
@Parameters(commandDescription = "Creates a new document for translation.")
final class CreateDocumentCmd extends DocumentCmd {
    @Parameter(
            names = {"-l", "--language"},
            description = "Language ID(s) separted by comma. "
                        + "The first element will be used as source language",
            required = true)
    private String languageIds;

    @Override
    protected void _execute() {
        String[] langs = languageIds.split(",");
        NewDocumentData newDocumentData = new NewDocumentData(langs[0].trim());
        if (langs.length > 1) {
            Set<String> targetLangs = new HashSet<String>(langs.length - 1);
            for (int i = 1; i < langs.length; i++) {
                targetLangs.add(langs[i].trim());
            }
            newDocumentData.setTargetLanguages(targetLangs);
        }

        try {
            getClient().createDocument(documentId, type, newDocumentData);
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }

        System.out.println("A new document '" + documentId + "' was successfully created.");
    }
}
