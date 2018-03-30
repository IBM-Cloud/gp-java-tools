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

import java.io.File;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.ibm.g11n.pipeline.client.ServiceException;

/**
 * Imports source language data to a translatable document.
 * 
 * @author John Emmons
 */
@Parameters(commandDescription = "Imports source language content to a translatable document.")
final class ImportDocumentCmd extends DocumentCmd {
    @Parameter(
            names = {"-f", "--file"},
            description = "File name to be imported",
            required = true)
    private String fileName;

    @Parameter(
            names = {"-l", "--language"},
            description = "Language ID",
            required = true)
    private String languageId;

    @Override
    protected void _execute() {
        File f = new File(fileName);

        try {
            getClient().updateDocumentContent(documentId, type, languageId, f);
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Document content from " + fileName
                + " was successfully imported to document:" + documentId);
    }
}
