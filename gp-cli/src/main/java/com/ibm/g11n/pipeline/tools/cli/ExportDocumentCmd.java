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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.ibm.g11n.pipeline.client.ServiceException;

/**
 * Exports document content from a translatable document.
 * 
 * @author John Emmons
 */
@Parameters(commandDescription = "Exports document content from a translatable document.")
final class ExportDocumentCmd extends DocumentCmd {
    @Parameter(
            names = {"-l", "--language"},
            description = "Language ID",
            required = true)
    private String languageId;

    @Parameter(
            names = {"-f", "--file"},
            description = "File name to be exported",
            required = true)
    private String fileName;


    @Override
    protected void _execute() {
        byte[] content;
        try {
            content = getClient().getDocumentContent(documentId, type, languageId);
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }

        File f = new File(fileName);
        try  {
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(content);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write the document content to " + fileName + ": " + e.getMessage(), e);
        }

        System.out.println("Document content from document:" + documentId
                + ", language: " + languageId + " was successfully saved to file "
                + fileName);
    }
}
