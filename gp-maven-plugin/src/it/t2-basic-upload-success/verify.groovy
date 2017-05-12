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

import java.io.*
import java.lang.reflect.*
import java.util.*
import java.util.regex.*
import com.ibm.g11n.pipeline.maven.*
import com.ibm.g11n.pipeline.client.*

try {
    println "verifying that upload succeeded"
    String location = bldDir + "/it/t2-basic-upload-success";
    String propfile = "setup.properties"
    def propertyMap = ITTools.loadProperties(location, propfile);
    Credentials creds = ITTools.getCredentials(location, propfile);
    ServiceClient  gpClient = ServiceClient.getInstance(
                        ServiceAccount.getInstance(
                                creds.getUrl(), creds.getInstanceId(),
                                creds.getUserId(), creds.getPassword()));
    println "Instantiated Service Client. Getting available bundles"
    def availBundleIds = gpClient.getBundleIds();
    String bundlesToBeCreatedCSV = propertyMap.get("createdbundles");
    def bundlesToBeCreated = bundlesToBeCreatedCSV.split(',') 
    bundlesToBeCreated.each{
    	bundle -> println "Bundle to be uploaded as per setup: ${bundle}"
    }
    availBundleIds.each{
    	bundle -> println "Uploaded bundle: ${bundle}"
    }
    // The intersection of uploaded bundles and bundles created for this test setup should be empty
    assert [] == (bundlesToBeCreated - availBundleIds)
} catch( Throwable t ) {
    t.printStackTrace()
    return false
}
