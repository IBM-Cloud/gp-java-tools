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

    private Map<String, ResourceFilterRegistryEntry> registry;

    private ResourceFilterFactory(ClassLoader cl) {
        Map<String, ResourceFilterRegistryEntry> map = new HashMap<>();
        // Walk through available provider implementations
        for (ResourceFilterProvider provider : ServiceLoader.load(ResourceFilterProvider.class, cl)) {
            Iterator<FilterInfo> filtItr = provider.getAvailableResourceFilters();
            while (filtItr.hasNext()) {
                FilterInfo filtInfo = filtItr.next();
                String id = filtInfo.getId().toUpperCase(Locale.ROOT);
                if (!map.containsKey(id)) {
                    map.put(id, new ResourceFilterRegistryEntry(filtInfo, provider));
                }
            }
        }
        // Add default filters
        ResourceFilterProvider defaultProvider = new DefaultResourceFilterProvider();
        Iterator<FilterInfo> filtItr = defaultProvider.getAvailableResourceFilters();
        while (filtItr.hasNext()) {
            FilterInfo filtInfo = filtItr.next();
            String id = filtInfo.getId().toUpperCase(Locale.ROOT);
            if (!map.containsKey(id)) {
                map.put(id, new ResourceFilterRegistryEntry(filtInfo, defaultProvider));
            }
        }
        registry = Collections.unmodifiableMap(map);
    }

    private static volatile ResourceFilterFactory DEFAULT_FACTORY = null;

    /**
     * Returns the default <code>ResourceFilterFactory</code> instance initialized
     * by the current thread's context class loader.
     * 
     * @return  The default <code>ResourceFilterFactory</code> instance.
     */
    public static ResourceFilterFactory getDefaultInstance() {
        if (DEFAULT_FACTORY == null) {
            synchronized(ResourceFilterFactory.class) {
                if (DEFAULT_FACTORY == null) {
                    DEFAULT_FACTORY = new ResourceFilterFactory(Thread.currentThread().getContextClassLoader());
                }
            }
        }
        return DEFAULT_FACTORY;
    }

    /**
     * Returns an instance of <code>ResourceFilterFactory</code> using the specified
     * <code>ClassLoader</code> to look up custom {@link ResourceFilterProvider} implementations.
     * 
     * @param cl    The <code>ClassLoader</code> used for looking up custom <code>ResourceFilterProvider</code>
     *              implementations.
     * @return  A new instance of <code>ResourceFilterFactory</code>.
     */
    public static ResourceFilterFactory getInstance(ClassLoader cl) {
        return new ResourceFilterFactory(cl);
    }

    /**
     * Returns an unmodifiable view of the set of all available resource filter IDs.
     * @return an unmodifiable view of the set of all available resource filter IDs.
     */
    public Set<String> availableFilterIds() {
        return Collections.unmodifiableSet(registry.keySet());
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
    public FilterInfo filterInfo(String filterId) {
        ResourceFilterRegistryEntry entry = registry.get(filterId.toUpperCase(Locale.ROOT));
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
    public ResourceFilter resourceFilter(String filterId) {
        ResourceFilterRegistryEntry entry = registry.get(filterId.toUpperCase(Locale.ROOT));
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
    public MultiBundleResourceFilter multiBundleResourceFilter(String filterId) {
        ResourceFilterRegistryEntry entry = registry.get(filterId.toUpperCase(Locale.ROOT));
        if (entry == null || entry.filterInfo.getType() != Type.MULTI) {
            return null;
        }
        return entry.provider.getMultiBundleResourceFilter(entry.filterInfo.getId());
    }

    //
    // Following static methods were introduced in the initial implementation. These methods are
    // preserved for backward compatibility support.
    //

    /**
     * Returns an unmodifiable view of the set of all available resource filter IDs by
     * the default factory.
     * <p>
     * This operation is equivalent to <code>getDefaultInstance().availableFilterIds()</code>.
     * @return an unmodifiable view of the set of all available resource filter IDs.
     */
    public static Set<String> getAvailableFilterIds() {
        return getDefaultInstance().availableFilterIds();
    }

    /**
     * Returns the resource filter information for the specified <code>filterId</code> by
     * the default factory.
     * <code>filterId</code> is case insensitive. If no matching filter is found, this
     * method returns <code>null</code>.
     * <p>
     * This operation is equivalent to <code>getDefaultInstance().filterInfo(filterId)</code>.
     * 
     * @param filterId  The resource filter ID, case insensitive.
     * @return  the resource filter information for the specified <code>filterId</code>,
     *          or <code>null</code> if no matching filter is found.
     */
    public static FilterInfo getFilterInfo(String filterId) {
        return getDefaultInstance().filterInfo(filterId);
    }

    /**
     * Returns an instance of {@link ResourceFilter} for the specified <code>filterId</code>
     * by the default factory.
     * <code>filterId</code> is case insensitive. If no matching filter is found, this
     * method returns <code>null</code>.
     * <p>
     * This operation is equivalent to <code>getDefaultInstance().resourceFilter(filterId)</code>.
     * 
     * @param filterId  The resource filter ID, case insensitive.
     * @return  an instance of {@link ResourceFilter} for the specified <code>filterId</code>,
     *          or <code>null</code> if no matching filter is found.
     */
    public static ResourceFilter getResourceFilter(String filterId) {
        return getDefaultInstance().resourceFilter(filterId);
    }

    /**
     * Returns an instance of {@link MultiBundleResourceFilter} for the specified <code>filterId</code>
     * by the default factory.
     * <code>filterId</code> is case insensitive. If no matching filter is found, this
     * method returns <code>null</code>.
     * <p>
     * This operation is equivalent to <code>getDefaultInstance().multiBundleResourceFilter(filterId)</code>.
     * 
     * @param filterId  The resource filter ID, case insensitive.
     * @return  an instance of {@link MultiBundleResourceFilter} for the specified <code>filterId</code>,
     *          or <code>null</code> if no matching filter is found.
     */
    public static MultiBundleResourceFilter getMultiBundleResourceFilter(String filterId) {
        return getDefaultInstance().multiBundleResourceFilter(filterId);
    }
}
