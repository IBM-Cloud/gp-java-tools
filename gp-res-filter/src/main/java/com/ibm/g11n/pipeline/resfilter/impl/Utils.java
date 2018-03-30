/*  
 * Copyright IBM Corp. 2018
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
package com.ibm.g11n.pipeline.resfilter.impl;

import java.text.BreakIterator;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.ibm.g11n.pipeline.resfilter.FilterOptions;
import com.ibm.g11n.pipeline.resfilter.ResourceString;

/**
 * Misc. static utility methods used by resource filter implementation
 * 
 * @author yoshito_umaoka
 */
final class Utils {
    static BreakIterator getWordBreakIterator(FilterOptions options) {
        Locale bitrLocale = Locale.ROOT;
        if (options  != null && options.getContentLocale() != null) {
            bitrLocale = options.getContentLocale();
        }
        return BreakIterator.getWordInstance(bitrLocale);
    }

    static Map<String, String> createKeyValueMap(Collection<ResourceString> resStrings) {
        Map<String, String> kvMap = new HashMap<String, String>(resStrings.size() * 4 / 3 + 1);
        for (ResourceString res : resStrings) {
            kvMap.put(res.getKey(), res.getValue());
        }
        return kvMap;
    }

    static Map<String, ResourceString> createResourceStringMap(Collection<ResourceString> resStrings) {
        Map<String, ResourceString> resMap = new HashMap<String, ResourceString>(resStrings.size() * 4 / 3 + 1);
        for (ResourceString resString : resStrings) {
            resMap.put(resString.getKey(), resString);
        }
        return resMap;
    }
}
