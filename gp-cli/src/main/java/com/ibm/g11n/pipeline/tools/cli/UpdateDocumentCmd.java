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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.ibm.g11n.pipeline.client.DocumentDataChangeSet;
import com.ibm.g11n.pipeline.client.ServiceException;

/**
 * Updates an existing translatable document.
 * 
 * @author John Emmons
 */
@Parameters(commandDescription = "Updates an existing translatable document.")
public class UpdateDocumentCmd extends DocumentCmd {

    @Parameter(
            names = {"-l", "--languages"},
            description = "List of document's target language ID(s) separted by comma. "
                        + "Empty list \"\" will remove all existing target languages.")
    private String languageIdsListStr;

    @Parameter(
            names = {"-n", "--note"},
            description = "Translation instruction notes. "
                        + "Empty note \"\" will remove existing instruction notes.")
    private String note;

    @Parameter(
            names = {"-r", "--readOnly"},
            description = "true to set the document read only.")
    private String readOnlyStr;

    @Override
    protected void _execute() {
        Set<String> trgLangs = null;
        if (languageIdsListStr != null) {
            String[] langs = languageIdsListStr.split(",");
            trgLangs = new HashSet<>(langs.length);
            for (String lang : langs) {
                if (lang.isEmpty()) {
                    continue;
                }
                trgLangs.add(lang);
            }
        }

        List<String> notes = null;
        if (note != null) {
            if (note.isEmpty()) {
                // Empty note will delete the existing note
                notes = Collections.emptyList();
            } else {
                notes = Collections.singletonList(note);
            }
        }

        Boolean readOnly = null;
        if (readOnlyStr != null) {
            if (readOnlyStr.equalsIgnoreCase("true")) {
                readOnly = Boolean.TRUE;
            } else if (readOnlyStr.equalsIgnoreCase("false")) {
                readOnly = Boolean.FALSE;
            } else {
                System.out.println("Bad -r (--readOnly) argument value: " + readOnlyStr
                        + ". The command argument will be ignored.");
            }
        }

        if (languageIdsListStr == null && notes == null && readOnlyStr == null) {
            System.out.println("Nothing to update.");
            return;
        }

        DocumentDataChangeSet changes = new DocumentDataChangeSet();
        changes
            .setTargetLanguages(trgLangs)
            .setNotes(notes)
            .setReadOnly(readOnly);

        try {
            getClient().updateDocument(type, documentId, changes);
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Document " + documentId + " was successfully updated.");
        if (trgLangs != null) {
            String newTrgLangs = trgLangs.isEmpty() ? "<removed>" : trgLangs.toString();
            System.out.println("- Target languages: " + newTrgLangs);
        }
        if (notes != null) {
            String newNote = notes.isEmpty() ? "<removed>" : notes.get(0);
            System.out.println("- Translation instruction note: " + newNote);
        }
        if (readOnly != null) {
            System.out.println("- Read only: " + readOnly);
        }
    }
}
