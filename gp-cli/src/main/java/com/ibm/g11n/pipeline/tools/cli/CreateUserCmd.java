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

import java.util.HashSet;
import java.util.Set;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.ibm.g11n.pipeline.client.NewUserData;
import com.ibm.g11n.pipeline.client.ServiceException;
import com.ibm.g11n.pipeline.client.UserData;
import com.ibm.g11n.pipeline.client.UserType;

/**
 * Creates a new service instance user.
 * 
 * @author Visaahan Anandarajah
 */
@Parameters(commandDescription = "Creates a new user.")
final class CreateUserCmd extends ServiceInstanceCmd {

    @Parameter(
            names = { "-t", "--type"},
            description = "User type, ADMINISTRATOR or TRANSLATOR or READER",
            required = true)
    private UserType userType;

    @Parameter(
            names = { "-d", "--displayName"},
            description = "User's display name",
            required = false)
    private String displayName;

    @Parameter(
            names = { "-c", "--comment"},
            description = "Comment",
            required = false)
    private String comment;
    
    @Parameter(
            names = { "-b", "--bundleNames"},
            description = "Bundle names that will be separated by a comma, or '*' for all bundles",
            required = false)
    protected String bundleNames;

    @Override
    protected void _execute() {
        NewUserData newUserData = new NewUserData(userType);

        newUserData.setComment(comment);
        newUserData.setDisplayName(displayName);

        if (bundleNames != null) {
            Set<String> bundleNameSet = new HashSet<String>();
            String[]  bundles = (bundleNames.indexOf(",") != -1) ? bundleNames.split(",") : new String[] {bundleNames};
            for(String bundle: bundles){
                bundleNameSet.add(bundle);
            }
            newUserData.setBundles(bundleNameSet);
        }

        try {
            UserData userData = getClient().createUser(newUserData);
            System.out.println("A new user was successfully created.");
            System.out.println("User ID: " + userData.getId());
            System.out.println("User password: " + userData.getPassword());
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }
}
