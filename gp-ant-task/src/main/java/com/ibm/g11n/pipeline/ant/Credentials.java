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
package com.ibm.g11n.pipeline.ant;

/**
 * Credentials class is used for receiving credential parameters
 * from maven plug-in configuration.
 * 
 * @author Yoshito Umaoka
 */
public class Credentials {
    private String url;
    
    private String instanceId;
    
    private String userId;

    private String password;

    public Credentials() {
    }

    public Credentials(String url, String instanceId, String userId, String password) {
        this.url = url;
        this.instanceId = instanceId;
        this.userId = userId;
        this.password = password;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the instanceId
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * @param instanceId the instanceId to set
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * @return the userId
     */
    public String getUserId() {
        return userId;
    }

    /**
     * @param userId the userId to set
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isValid() {
        return url != null && instanceId != null && userId != null && password != null;
    }

    @Override
    public String toString() {
        return "[url=" + url + ", instanceId=" + instanceId + ", userID=" + userId
                + ", password=" + password + "]";
    }
}
