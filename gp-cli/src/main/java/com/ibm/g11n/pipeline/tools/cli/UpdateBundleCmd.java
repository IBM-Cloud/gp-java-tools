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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.ibm.g11n.pipeline.client.BundleDataChangeSet;
import com.ibm.g11n.pipeline.client.ServiceException;

/**
 * Updates an existing translation bundle.
 * 
 * @author Yoshito Umaoka
 */
@Parameters(commandDescription = "Updates an existing translation bundle.")
public class UpdateBundleCmd extends BundleCmd {

    @Parameter(
            names = {"-l", "--languages"},
            description = "List of bundle's target language ID(s) separted by comma.")
    private String languageIdsListStr;

    @Parameter(
            names = {"-n", "--note"},
            description = "Translation instruction note")
    private String note;

    @Override
    protected void _execute() {
        Set<String> trgLangs = null;
        if (languageIdsListStr != null) {
            String[] langs = languageIdsListStr.split(",");
            trgLangs = new HashSet<>(langs.length);
            for (String lang : langs) {
                trgLangs.add(lang);
            }
        }

        if (languageIdsListStr == null && note == null) {
            System.out.println("Nothing to update.");
            return;
        }

        BundleDataChangeSet changes = new BundleDataChangeSet();
        changes
            .setTargetLanguages(trgLangs)
            .setNotes(Collections.singletonList(note));

        try {
            getClient().updateBundle(bundleId, changes);
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Bundle " + bundleId + " was successfully updated.");
        if (trgLangs != null) {
            System.out.println("- Target languages: " + trgLangs);
        }
        if (note != null) {
            System.out.println("- Translation instruction note: " + note);
        }
    }
}
