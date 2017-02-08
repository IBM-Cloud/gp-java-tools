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
 * Language ID styles.
 * 
 * @author Yoshito Umaoka
 */
public enum LanguageIdStyle {
    /**
     * BCP 47 language tag. For example, "pt-BR" for Brazilian Portuguese
     */
    BCP47,

    /**
     * Modified version of BCP 47 language tag, using '_' for separating
     * subtags instead of '-'. For example, "pt_BR".
     */
    BCP47_UNDERSCORE,
}
