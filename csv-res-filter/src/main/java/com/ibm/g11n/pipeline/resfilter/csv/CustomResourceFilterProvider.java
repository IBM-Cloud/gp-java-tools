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
package com.ibm.g11n.pipeline.resfilter.csv;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import com.ibm.g11n.pipeline.resfilter.FilterInfo;
import com.ibm.g11n.pipeline.resfilter.MultiBundleResourceFilter;
import com.ibm.g11n.pipeline.resfilter.ResourceFilter;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterProvider;

public class CustomResourceFilterProvider extends ResourceFilterProvider {

    private static final List<FilterInfo> FILTERS = Collections.unmodifiableList(
            Arrays.asList(
                    new FilterInfo(CSVFilter.TYPE, CSVFilter.ID),
                    new FilterInfo(MultiBundleCSVFilter.TYPE, MultiBundleCSVFilter.ID)));

    @Override
    public Iterator<FilterInfo> getAvailableResourceFilters() {
        return FILTERS.iterator();
    }

    @Override
    public ResourceFilter getResourceFilter(String id) {
        if (id.toUpperCase(Locale.ROOT).equals(CSVFilter.ID)) {
            return new CSVFilter();
        }
        return null;
    }

    @Override
    public MultiBundleResourceFilter getMultiBundleResourceFilter(String id) {
        if (id.toUpperCase(Locale.ROOT).equals(MultiBundleCSVFilter.ID)) {
            return new MultiBundleCSVFilter();
        }
        return null;
    }

}
