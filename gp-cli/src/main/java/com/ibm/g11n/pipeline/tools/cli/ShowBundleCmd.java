/*  
 * Copyright IBM Corp. 2015
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

import com.beust.jcommander.Parameters;
import com.ibm.g11n.pipeline.client.BundleData;
import com.ibm.g11n.pipeline.client.ServiceException;

/**
 * Prints out a translation bundle's information.
 * 
 * @author Yoshito Umaoka
 */
@Parameters(commandDescription = "Prints out a translation bundle's information.")
final class ShowBundleCmd extends BundleCmd {
    @Override
    protected void _execute() {
        try {
            BundleData bundleData = getClient().getBundleInfo(bundleId);

            System.out.println("Bundle ID:        " + bundleId);
            System.out.println("Source Language:  " + bundleData.getSourceLanguage());
            System.out.println("Target Languages: " + bundleData.getTargetLanguages());
            System.out.println("Read Only:        " + bundleData.isReadOnly());
            if (bundleData.getPartner() != null) {
                System.out.println("Partner:          " + bundleData.getPartner());
            }
            System.out.println("Updated by:       " + bundleData.getUpdatedBy());
            System.out.println("Updated at:       " + bundleData.getUpdatedAt());
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }
}
