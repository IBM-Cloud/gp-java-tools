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

/**
 * Class to support credentials from the fake broker
 * @author jugudanniesundar
 *
 */
public class CredentialsExtended {
    
    private Credentials credentials;
    
    private int timeout;
    
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
     * @return the timeout
     */
    public int getTimeout() {
        return timeout;
    }
    
    /**
     * @param timeout the timeout to set
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    

}
