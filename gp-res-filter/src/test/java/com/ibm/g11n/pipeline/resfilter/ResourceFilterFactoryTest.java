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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.Set;

import org.junit.Test;

import com.ibm.g11n.pipeline.resfilter.FilterInfo.Type;

/**
 * Test cases for {@link ResourceFilterFactory}.
 * 
 * @author yoshito_umaoka
 */
public class ResourceFilterFactoryTest {

    @Test
    public void testFactory() {
        Set<String> availableFilters = ResourceFilterFactory.getAvailableFilterIds();
        assertTrue("There should be some filters available by default", availableFilters.size() > 0);

        for (String id : availableFilters) {
            FilterInfo filterInfo = ResourceFilterFactory.getFilterInfo(id);
            assertNotNull("FilterInfo must be available for " + id, filterInfo);

            String idLowerCase = id.toLowerCase(Locale.ROOT);
            filterInfo = ResourceFilterFactory.getFilterInfo(idLowerCase);
            assertNotNull("getFilterInfo(id) should be case insensitive", filterInfo);

            Type filterType = filterInfo.getType();
            switch (filterType) {
            case SINGLE:
                assertNotNull("ResourceFilter should be available for " + id,
                        ResourceFilterFactory.getResourceFilter(id));
                assertNull("MultiBundleResourceFilter should not be available for " + id,
                        ResourceFilterFactory.getMultiBundleResourceFilter(id));
                break;
            case MULTI:
                assertNull("ResourceFilter should not be available for " + id,
                        ResourceFilterFactory.getResourceFilter(id));
                assertNotNull("MultiBundleResourceFilter should be available for " + id,
                        ResourceFilterFactory.getMultiBundleResourceFilter(id));
                break;
            }
        }

        String bogusId = "bogus";
        assert !availableFilters.contains(bogusId);
        assertNull("FilterInfo for bogus should not be available", ResourceFilterFactory.getFilterInfo(bogusId));
    }
}
