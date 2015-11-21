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

import java.util.Map;
import java.util.Map.Entry;

import com.beust.jcommander.Parameters;
import com.ibm.g11n.pipeline.client.ServiceException;
import com.ibm.g11n.pipeline.client.UserData;

/**
 * Prints out a list of users.
 * 
 * @author Visaahan Anandarajah
 */
@Parameters(commandDescription = "Prints out a list of users.")
final class ListUsersCmd extends ServiceInstanceCmd{
    @Override
    protected void _execute() {
        try {
            Map<String, UserData> users = getClient().getUsers();
            if (users.size() == 0) {
                System.out.println("No users found.");
            } else {
                for (Entry<String, UserData> entry : users.entrySet()) {
                    String userId = entry.getKey();
                    UserData userData = entry.getValue();
                    StringBuilder out = new StringBuilder(userId);
                    out.append("[").append(userData.getType()).append("]");
                    String displayName = userData.getDisplayName();
                    if (displayName != null) {
                        out.append(" ").append(displayName);
                    }
                    System.out.println(out);
                }
            }
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }
}
