/*  
 * Copyright IBM Corp. 2015,2016
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.ibm.g11n.pipeline.client.ServiceAccount;
import com.ibm.g11n.pipeline.client.ServiceClient;

/**
 * The root class of Globalization Pipeline command.
 * 
 * @author Yoshito Umaoka
 */
abstract class BaseCmd {
    @Parameter(
            names = {"-j", "--jsonCreds"},
            description = "JSON file containing Globalization Pipeline service credentials")
    protected String jsonCreds;

    @Parameter(
            names = {"-s", "--serviceUrl"},
            description = "Globalization Pipeline service URL")
    protected String gpUrl;

    @Parameter(
            names = {"-i", "--instanceId"},
            description = "Service instance ID")
    protected String instanceId;

    @Parameter(
            names = {"-u", "--user"},
            description = "User ID")
    protected String userId;

    @Parameter(
            names = {"-p", "--password"},
            description = "Password")
    protected String password;

    protected abstract void _execute();

    static class JsonCredentials {
        String url;
        String instanceId;
        String userId;
        String password;
    }

    protected ServiceClient getClient() {
        if (jsonCreds != null) {
            JsonCredentials creds;
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(jsonCreds), StandardCharsets.UTF_8)) {
                Gson gson = new Gson();
                creds = gson.fromJson(reader, JsonCredentials.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // If individual credential fields are not specified by
            // command line options, use the one from the JSON credentials
            // file.
            if (gpUrl == null) {
                gpUrl = creds.url;
            }
            if (instanceId == null) {
                instanceId = creds.instanceId;
            }
            if (userId == null) {
                userId = creds.userId;
            }
            if (password == null) {
                password = creds.password;
            }
        }

        ServiceAccount account = ServiceAccount.getInstance(
                gpUrl, instanceId, userId, password);
        return ServiceClient.getInstance(account);
    }

    public void execute() {
        try {
            _execute();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
