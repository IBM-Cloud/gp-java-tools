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

/**
 * <code>FilterInfo</code> is used for specifying resource filter type and
 * ID.
 * @see ResourceFilterFactory#getFilterInfo(String)
 * @author yoshito_umaoka
 */
public final class FilterInfo {
    /**
     * Resource filter type enum
     */
    public enum Type {
        /**
         * A type of resource filter supporting single bundle only.
         * @see ResourceFilter
         */
        SINGLE,
        /**
         * A type of resource filter supporting multiple bunles.
         * @see MultiBundleResourceFilter
         */
        MULTI
    }

    private final Type type;
    private final String id;

    /**
     * Constructor
     * 
     * @param type  A type of resource filter
     * @param id    An id of resource filter
     */
    public FilterInfo(Type type, String id) {
        this.type = type;
        this.id = id;
    }

    /**
     * Returns the resource filter type
     * @return  the resource filter type
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the resource filter ID
     * @return  the resource filter ID
     */
    public String getId() {
        return id;
    }
}
