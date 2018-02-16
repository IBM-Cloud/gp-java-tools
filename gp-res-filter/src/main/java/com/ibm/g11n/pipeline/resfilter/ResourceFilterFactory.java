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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import com.ibm.g11n.pipeline.resfilter.FilterInfo.Type;
import com.ibm.g11n.pipeline.resfilter.impl.DefaultResourceFilterProvider;

/**
 * The resource filter service factory.
 * 
 * @author yoshito_umaoka
 */
public class ResourceFilterFactory {

    private static class ResourceFilterRegistryEntry {
        FilterInfo filterInfo;
        ResourceFilterProvider provider;

        ResourceFilterRegistryEntry(FilterInfo filterInfo, ResourceFilterProvider provider) {
            this.filterInfo = filterInfo;
            this.provider = provider;
        }
    }

    private static final Map<String, ResourceFilterRegistryEntry> REGISTRY = new HashMap<>();

    static {
        // Walk through available provider implementations
        for (ResourceFilterProvider provider : ServiceLoader.load(ResourceFilterProvider.class)) {
            Iterator<FilterInfo> filtItr = provider.getAvailableResourceFilters();
            while (filtItr.hasNext()) {
                FilterInfo filtInfo = filtItr.next();
                String id = filtInfo.getId().toUpperCase(Locale.ROOT);
                if (!REGISTRY.containsKey(id)) {
                    REGISTRY.put(id, new ResourceFilterRegistryEntry(filtInfo, provider));
                }
            }
        }
        // Add default filters
        ResourceFilterProvider defaultProvider = new DefaultResourceFilterProvider();
        Iterator<FilterInfo> filtItr = defaultProvider.getAvailableResourceFilters();
        while (filtItr.hasNext()) {
            FilterInfo filtInfo = filtItr.next();
            String id = filtInfo.getId().toUpperCase(Locale.ROOT);
            if (!REGISTRY.containsKey(id)) {
                REGISTRY.put(id, new ResourceFilterRegistryEntry(filtInfo, defaultProvider));
            }
        }
    }

    /**
     * Returns an unmodifiable view of the set of all available resource filter IDs.
     * @return an unmodifiable view of the set of all available resource filter IDs.
     */
    public static Set<String> getAvailableFilterIds() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }

    /**
     * Returns the resource filter information for the specified <code>filterId</code>.
     * <code>filterId</code> is case insensitive. If no matching filter is found, this
     * method returns <code>null</code>.
     * 
     * @param filterId  The resource filter ID, case insensitive.
     * @return  the resource filter information for the specified <code>filterId</code>,
     *          or <code>null</code> if no matching filter is found.
     */
    public static FilterInfo getFilterInfo(String filterId) {
        ResourceFilterRegistryEntry entry = REGISTRY.get(filterId.toUpperCase(Locale.ROOT));
        if (entry == null) {
            return null;
        }
        return entry.filterInfo;
    }

    /**
     * Returns an instance of {@link ResourceFilter} for the specified <code>filterId</code>.
     * <code>filterId</code> is case insensitive. If no matching filter is found, this
     * method returns <code>null</code>.
     * 
     * @param filterId  The resource filter ID, case insensitive.
     * @return  an instance of {@link ResourceFilter} for the specified <code>filterId</code>,
     *          or <code>null</code> if no matching filter is found.
     */
    public static ResourceFilter getResourceFilter(String filterId) {
        ResourceFilterRegistryEntry entry = REGISTRY.get(filterId.toUpperCase(Locale.ROOT));
        if (entry == null || entry.filterInfo.getType() != Type.SINGLE) {
            return null;
        }
        return entry.provider.getResourceFilter(entry.filterInfo.getId());
    }

    /**
     * Returns an instance of {@link MultiBundleResourceFilter} for the specified <code>filterId</code>.
     * <code>filterId</code> is case insensitive. If no matching filter is found, this
     * method returns <code>null</code>.
     * 
     * @param filterId  The resource filter ID, case insensitive.
     * @return  an instance of {@link MultiBundleResourceFilter} for the specified <code>filterId</code>,
     *          or <code>null</code> if no matching filter is found.
     */
    public static MultiBundleResourceFilter getMultiBundleResourceFilter(String filterId) {
        ResourceFilterRegistryEntry entry = REGISTRY.get(filterId.toUpperCase(Locale.ROOT));
        if (entry == null || entry.filterInfo.getType() != Type.MULTI) {
            return null;
        }
        return entry.provider.getMultiBundleResourceFilter(entry.filterInfo.getId());
    }
}
