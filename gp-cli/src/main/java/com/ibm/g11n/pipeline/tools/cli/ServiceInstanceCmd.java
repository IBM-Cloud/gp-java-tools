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

import com.beust.jcommander.Parameter;
import com.ibm.g11n.pipeline.client.ServiceAccount;
import com.ibm.g11n.pipeline.client.ServiceClient;

/**
 * A base class of service instance commands.
 * 
 * @author Visaahan Anandarajah
 */
public abstract class ServiceInstanceCmd extends AuthCmd {
    @Parameter(
            names = {"-i", "--instanceId"},
            description = "Service instance ID",
            required = true)
    protected String instanceId;

    @Override
    protected ServiceClient getClient() {
        ServiceAccount account = ServiceAccount.getInstance(
                gpUrl, instanceId, userId, password);
        return ServiceClient.getInstance(account);
    }
}
