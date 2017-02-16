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
 * This class is used within BundleSet as nested element for ant. 
 * This specifies the modifications to be made to the folder/file names 
 * when downloading translation files from the globalization pipeline instance
 * 
 * @author jugudanniesundar
 *
 */
public class LanguageMap {

    /**
     * The language name which needs to be modified when downloading files in that language
     */
    private String from;

    /**
     * The language name which should be used when downloading files in "from" language
     */
    private String to;

    /**
     * @return from     A language mapped from
     */
    public String getFrom() {
        return from;
    }

    /**
     * @param from  A language mapped from
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * @return to   A language mapped to
     */
    public String getTo() {
        return to;
    }

    /**
     * @param to    A language mapped to
     */
    public void setTo(String to) {
        this.to = to;
    }
}
