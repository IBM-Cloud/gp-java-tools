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

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.google.gson.Gson;

/**
 * Class containing methods/utilities to setup/verify integrating tests
 * 
 * @author jugudanniesundar
 *
 */

public class ITTools {
    
    /**
     * Method to load properties from a file into a map of key, value pairs
     * @param location The project location
     * @param propfile The setup properties file name
     * @return map of key, value for properties
     * @throws Exception if file doesn't exist
     */
    public static Map<String, String> loadProperties(String location, String propfile) throws Exception{
        Map<String, String> propMap = new HashMap<String, String>();
        // looking for "setup.properties" file in the integration test project folder speficied by location parameter
        String propertiesFile = location + "/" + propfile;
        Properties properties = new Properties();
        // looking for "setup.properties" file in the integration test project folder speficied by location parameter
        properties.load( new FileReader( propertiesFile ) );
        String url = "";
        String credentialsfilename = "credentials.json";
        
        for ( String propertyName : properties.stringPropertyNames() ) {
            if ("broker-url".equals(propertyName)) {
                url = properties.getProperty( propertyName );
                propMap.put("url", url);
            } else if ("credentials".equals(propertyName)) {
                credentialsfilename = properties.getProperty( propertyName );
                propMap.put("credentials", credentialsfilename);
            } else {
                propMap.put(propertyName, properties.getProperty( propertyName ));
            }
            
        }
        return propMap;
    }
    /**
     * Method to generate credentials using the setup file
     * @param location The project location
     * @param propfile The setup properties file name
     * @throws Exception if the properties of url, credentials don't exist
     */
    public static void createCredentialsFile(String location, String propfile) throws Exception {
        System.out.println("Creating credentials file using fake broker");
        Map<String, String> propMap = loadProperties(location, propfile);
        String url = propMap.get("url");
        String credentialsfilename = propMap.get("credentials");
        GPInstance gpInstance = GPInstance.getInstance(url);
        Credentials credentials = gpInstance.getCredentials();
        if (!credentials.getUrl().endsWith("/rest"))
            credentials.setUrl(credentials.getUrl() + "/rest"); // adding suffix if /rest is missing
        Gson gson = new Gson();
        String json = gson.toJson(credentials);
        PrintWriter out = new PrintWriter(location + "/" + credentialsfilename);
        out.print(json);
        System.out.println("Created Credentials file successfully");
        out.close();
    }
    
    /**
     * Method to get credentials by reading the credentials.json file
     * @param location The location of the project folder
     * @param propfile The setup properties file name
     * @return Credentials The GP instance credentials
     * @throws Exception If the credentials file doesn't exist
     */
    public static Credentials getCredentials(String location, String propfile) throws Exception {
        Map<String, String> propMap = loadProperties(location, propfile);
        String credentialsfile = location + "/" + propMap.get("credentials"); 
        InputStreamReader reader = new InputStreamReader(new FileInputStream(credentialsfile), StandardCharsets.UTF_8);
        Gson gson = new Gson();
        Credentials creds = gson.fromJson(reader, Credentials.class);
        return creds;
    }  

}
