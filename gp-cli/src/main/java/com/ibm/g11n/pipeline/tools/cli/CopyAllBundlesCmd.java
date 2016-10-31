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

import java.util.Set;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.ibm.g11n.pipeline.client.ServiceAccount;
import com.ibm.g11n.pipeline.client.ServiceClient;
import com.ibm.g11n.pipeline.client.ServiceException;

@Parameters(commandDescription = "Copies all bundle data to another service instance.")
class CopyAllBundlesCmd extends BaseCmd {
    @Parameter(
            names = { "--dest-url"},
            description = "The destinaion service instance's URL",
            required = true)
    private String destUrl;

    @Parameter(
            names = { "--dest-instance-id"},
            description = "The destination service instance ID",
            required = true)
    private String destInstanceId;

    @Parameter(
            names = { "--dest-user-id"},
            description = "User ID used for the destination service instance",
            required = true)
    private String destUserId;

    @Parameter(
            names = { "--dest-password"},
            description = "Password used for the destination service instance",
            required = true)
    private String destPassword;

    @Override
    protected void _execute() {
        try {
            ServiceAccount destAccount = ServiceAccount.getInstance(destUrl, destInstanceId,
                    destUserId, destPassword);
            ServiceClient destClient = ServiceClient.getInstance(destAccount);
            ServiceClient srcClient = getClient();

            // First, get a list of bundles
            Set<String> bundleIds = srcClient.getBundleIds();
            for (String bundleId : bundleIds) {
                // Process each bundle
                System.out.println(".. copying bundle: " + bundleId);
                CopyBundleCmd.copyBundle(srcClient, bundleId, destClient, bundleId);
            }
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }

        System.out.println("All bundles were successfully copied to the specified service instance.");
    }

}
