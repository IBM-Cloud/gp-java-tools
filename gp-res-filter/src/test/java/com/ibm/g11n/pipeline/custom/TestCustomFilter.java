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
package com.ibm.g11n.pipeline.custom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Test;

import com.ibm.g11n.pipeline.resfilter.FilterOptions;
import com.ibm.g11n.pipeline.resfilter.LanguageBundle;
import com.ibm.g11n.pipeline.resfilter.LanguageBundleBuilder;
import com.ibm.g11n.pipeline.resfilter.ResourceFilter;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterException;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterFactory;
import com.ibm.g11n.pipeline.resfilter.ResourceString;
import com.ibm.g11n.pipeline.resfilter.ResourceString.ResourceStringComparator;
import com.ibm.g11n.pipeline.resfilter.impl.ResourceTestUtil;

/**
 * Custom filter test cases
 * 
 * @author Yoshito Umaoka
 */
public class TestCustomFilter {
    @Test
    public void testFactory() {
        Set<String> availableIDs = ResourceFilterFactory.getAvailableFilterIds();
        assertTrue("Available filter IDs contains " + MockResourceFilter.ID, availableIDs.contains(MockResourceFilter.ID));

        ResourceFilter filter = ResourceFilterFactory.getResourceFilter(MockResourceFilter.ID);
        assertNotNull("Resource filter for " + MockResourceFilter.ID, filter);
        assertEquals("Resource filter class", MockResourceFilter.class, filter.getClass());
    }

    private static final File INPUT_FILE = new File("src/test/resource/resfilter/mock/input.mock");
    private static final String EXPECTED_LANGCODE = "en_US";
    private static final List<String> EXPECTED_BUNDLE_NOTES;
    static {
        EXPECTED_BUNDLE_NOTES = new LinkedList<>();
        EXPECTED_BUNDLE_NOTES.add("Input test case");
        EXPECTED_BUNDLE_NOTES.add("See com.ibm.g11n.pipeline.custom.MockResourceFilter");
    }
    private static final Map<String, String> EXPECTED_BUNDLE_METADATA;
    static {
        EXPECTED_BUNDLE_METADATA = new TreeMap<>();
        EXPECTED_BUNDLE_METADATA.put("test", "true");
        EXPECTED_BUNDLE_METADATA.put("component", "gp-res-filter");
    }

    private static final Collection<ResourceString> EXPECTED_INPUT_RES_LIST;
    static {
        List<ResourceString> lst = new LinkedList<>();

        lst.add(ResourceString.with("key1", "rose")
                .addNote("flower").addMetadata("color", "red")
                .sequenceNumber(1).build());
        lst.add(ResourceString.with("key2", "banana")
                .addNote("fruit").addNote("toropical")
                .addMetadata("fresh", "true").addMetadata("color", "yellow")
                .sequenceNumber(2).build());

        Collections.sort(lst, new ResourceStringComparator());
        EXPECTED_INPUT_RES_LIST = lst;
    }

    @Test
    public void testParse() throws IOException, ResourceFilterException {
        assertTrue("The input test file <" + INPUT_FILE + "> does not exist.", INPUT_FILE.exists());

        ResourceFilter filter = ResourceFilterFactory.getResourceFilter("MOCK");
        try (InputStream is = new FileInputStream(INPUT_FILE)) {
            LanguageBundle bundle = filter.parse(is, new FilterOptions(Locale.ENGLISH));

            assertEquals("Embedded language code", EXPECTED_LANGCODE, bundle.getEmbeddedLanguageCode());
            assertEquals("Bundle notes", EXPECTED_BUNDLE_NOTES, bundle.getNotes());
            assertEquals("Bundle metadata", EXPECTED_BUNDLE_METADATA, bundle.getMetadata());

            List<ResourceString> resStrList = new ArrayList<>(bundle.getResourceStrings());
            Collections.sort(resStrList, new ResourceStringComparator());
            assertEquals("ResourceStrings did not match.", EXPECTED_INPUT_RES_LIST, resStrList);
        }
    }

    private static LanguageBundle WRITE_BUNDLE;

    static {
        LanguageBundleBuilder bundleBuilder = new LanguageBundleBuilder(true);

        bundleBuilder.embeddedLanguageCode("ja-JP");
        bundleBuilder.addNote("Test case for write");
        bundleBuilder.addNote("Mock resource filter");
        bundleBuilder.addMetadata("test", "true");
        bundleBuilder.addMetadata("date", "2018-03-30");

        bundleBuilder.addResourceString(
                ResourceString.with("first", "最初").addNote("note1").addMetadata("mk1", "mv1"));

        bundleBuilder.addResourceString(
                ResourceString.with("middle", "中間").addNote("note2-1").addNote("note2-2"));

        bundleBuilder.addResourceString(
                ResourceString.with("last", "最後").addMetadata("mk3-1", "mv3-1").addMetadata("mk3-2", "mv3-2"));

        WRITE_BUNDLE = bundleBuilder.build();
    }

    private static final File EXPECTED_WRITE_FILE = new File("src/test/resource/resfilter/mock/write-output.mock");

    @Test
    public void testWrite() throws IOException, ResourceFilterException {
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".mock");
        tempFile.deleteOnExit();

        ResourceFilter filter = ResourceFilterFactory.getResourceFilter("MOCK");
        try (OutputStream os = new FileOutputStream(tempFile)) {
            filter.write(os, WRITE_BUNDLE, new FilterOptions(Locale.ENGLISH));
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_WRITE_FILE, tempFile));
        }
    }
}
