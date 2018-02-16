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
package com.ibm.g11n.pipeline.resfilter;

import java.util.Locale;
import java.util.Map;

/**
 * An instance of <code>FileterOptions</code> stores options used to
 * control resource filter's behavior.
 * 
 * @see ResourceFilter
 * @see MultiBundleResourceFilter
 * @author yoshito_umaoka
 */
public class FilterOptions {
    private Locale contentLocale;
    private Map<String, String> customParams;

    /**
     * Constructs a <code>FilterOptions</code> instance.
     * 
     * @param contentLocale A locale used for processing resource contents. For example,
     *                      this locale might be used for creating a <code>BreakIterator</code>
     *                      for text wrapping.
     */
    public FilterOptions(Locale contentLocale) {
        this.contentLocale = contentLocale;
    }

    /**
     * Returns the locale used for processing resource contents.
     * @return  the locale used for processing resource contents.
     */
    public Locale getContentLocale() {
        return contentLocale;
    }

    /**
     * Sets a map including key-value pairs specifying filter implementation specific custom
     * parameters.
     * <p>
     * Note: This map is not currently used by the default filter implementations included
     * in this library.
     * 
     * @param customParams  A map including key-value pairs specifying filter implementation
     *                      specific custom parameters.
     */
    public void setCustomParams(Map<String, String> customParams) {
        this.customParams = customParams;
    }

    /**
     * Returns a map including key-value pairs specifying filter implementation specific
     * custom parameters.
     * 
     * @return  a map including key-value pairs specifying filter implementation specific
     * custom parameters.
     */
    public Map<String, String> getCustomParams() {
        return customParams;
    }
}
