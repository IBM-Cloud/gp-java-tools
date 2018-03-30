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

import java.util.Iterator;

import com.ibm.g11n.pipeline.resfilter.FilterInfo.Type;

/**
 * Service provider of resource filter.
 * <p>
 * A concrete subclass provides custom resource filter implementation used
 * for importing string resource contents to Globalization Pipeline, or for exporting
 * resource data from Globalization Pipeline to a resource file.
 * 
 * @author yoshito_umaoka
 */
public abstract class ResourceFilterProvider {

    /**
     * Returns an iterator over all filter type information supported by this provider.
     * 
     * @return an iterator over all filter type information supported by this provider.
     */
    public abstract Iterator<FilterInfo> getAvailableResourceFilters();

    /**
     * Returns an instance of {@link ResourceFilter} specified by <code>id</code>.
     * <p>
     * The resource filter service calls {@link #getAvailableResourceFilters()} first.
     * This method is called only when {@link #getAvailableResourceFilters()} includes
     * FilterInfo with the <code>id</code> and filter type is {@link Type#SINGLE}.
     * 
     * @param id    A resource filter ID.
     * @return  an instance of {@link ResourceFilter} specified by <code>id</code>, or null
     *          when an implementation of {@link ResourceFilter} specified by <code>id</code>
     *          is not supported by this provider.
     */
    public abstract ResourceFilter getResourceFilter(String id);

    /**
     * Returns an instance of {@link MultiBundleResourceFilter} specified by <code>id</code>.
     * <p>
     * The resource filter service calls {@link #getAvailableResourceFilters()} first.
     * This method is called only when {@link #getAvailableResourceFilters()} includes
     * FilterInfo with the <code>id</code> and filter type is {@link Type#MULTI}.
     * 
     * @param id    A resource filter ID.
     * @return  an instance of {@link MultiBundleResourceFilter} specified by <code>id</code>,
     *          or null when an implementation of {@link MultiBundleResourceFilter} specified by
     *          <code>id</code> is not supported by this provider.
     */
    public abstract MultiBundleResourceFilter getMultiBundleResourceFilter(String id);
}
