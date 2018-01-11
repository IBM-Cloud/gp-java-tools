/*  
 * Copyright IBM Corp. 2016, 2018
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
 * Resource bundle file layout types.
 * 
 * @author Yoshito Umaoka
 */
public enum BundleLayout {
    /**
     * In the same directory with the source, with extra language suffix.
     * For example, if the source file is com/ibm/g11n/MyMessages.properties,
     * then the French version will be com/ibm/g11n/MyMessages_fr.properties.
     */
    LANGUAGE_SUFFIX,

    /**
     * In the same directory with the source, using language code as the
     * file name.
     * For example, if the source file is com/ibm/g11n/en.json,
     * then the French version will be com/ibm/g11n/fr.json.
     * <p>
     * Note: With this option, output file name will be just language code
     * (with file extension same with the source file) regardless of the source
     * file name.
     */
    LANGUAGE_ONLY,

    /**
     * In a language sub-directory of the director where the source file is.
     * For example, if the source file is com/ibm/g11n/MyMessages.json,
     * then the French version will be com/ibm/g11n/fr/MyMessages.json.
     */
    LANGUAGE_SUBDIR,

    /**
     * In a language directory at the same level with the source file.
     * For example, if the source file is com/ibm/g11n/en/MyMessages.properties,
     * then the French version will be com/ibm/g11n/fr/MyMessages.properties.
     */
    LANGUAGE_DIR
}
