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
package com.ibm.g11n.pipeline.maven;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Class to generate GP instance from the fake broker
 * 
 * @author jugudanniesundar
 *
 */
public class GPInstance {
    
    private static GPInstance gpInstance;
    
    private Credentials credentials;
    
    /**
     * @return the credentials
     */
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * @param credentials the credentials to set
     */
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    /**
     * private constructor to support singleton behaviour
     */
    private GPInstance() {
    }
    
    /**
     * Generating instance from the fakebroker url
     * @param url The fakebroker url
     * @return Globalization pipeline instance having credentials
     * @throws Exception if connection fails or if the response is not formatted as json
     */
    public static GPInstance getInstance(String url) throws Exception{
        if (gpInstance == null) {
            synchronized (GPInstance.class) {
                if (gpInstance == null) {
                    try {
                        URL fakeBrokerURL = new URL(url);    
                        HttpURLConnection request = (HttpURLConnection) fakeBrokerURL.openConnection();
                        request.connect();
                        InputStreamReader reader = new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8);
                        Gson gson = new Gson();
                        CredentialsExtended credsExt = gson.fromJson(reader, CredentialsExtended.class);
                        gpInstance = new GPInstance();
                        gpInstance.setCredentials(credsExt.getCredentials());
                    } catch (MalformedURLException e) { 
                        throw new Exception("Bad URL Exception");
                    } catch (IOException e) {
                        throw new Exception("IO Exception");
                    } catch (JsonSyntaxException e) {
                        throw new Exception("Format Exception");
                    }
                }
            }
        }
        return gpInstance;
    }
        
}
