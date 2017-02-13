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
package com.ibm.g11n.pipeline.ant;

/**
 * This class is used within BundleSet as nested element.
 * This class is also used in GPUploadTask/GPDownloadTask as nested element
 * 
 * When used within BundleSet, then the languages specified using TargetLanguages
 * are used to upload/download files specifically in that language in that bundle
 * 
 * If the targetLanguages are not defined within the BundleSet, then the targetLanguages
 * defined in GPUploadTask/GPDownloadTask is used for uploading/downloading files in those language(s)
 * 
 * @author jugudanniesundar
 *
 */
public class TargetLanguage {

    /**
     * a target language
     */
    String lang;

    public TargetLanguage() {
    }

    /**
     * 
     * @param targetLang
     */
    public void setLang(String targetLang) { 
        this.lang = targetLang; 
    }

    /**
     * 
     * @return targetLang
     */
    public String getLang() { 
        return lang; 
    }
}
