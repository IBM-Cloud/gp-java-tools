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
package com.ibm.g11n.pipeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;

import com.ibm.g11n.pipeline.resfilter.FilterOptions;
import com.ibm.g11n.pipeline.resfilter.LanguageBundle;
import com.ibm.g11n.pipeline.resfilter.MultiBundleResourceFilter;
import com.ibm.g11n.pipeline.resfilter.ResourceFilter;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterException;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterFactory;
import com.ibm.g11n.pipeline.resfilter.ResourceString;

public class TestMultiBundleCSVFilter {
    @Test
    public void testFactory() {
        Set<String> availableIDs = ResourceFilterFactory.getAvailableFilterIds();
        assertTrue("Available filter IDs contains " + MultiBundleCSVFilter.ID, availableIDs.contains(MultiBundleCSVFilter.ID));

        ResourceFilter filter = ResourceFilterFactory.getResourceFilter(MultiBundleCSVFilter.ID);
        assertNull("Multi-bundle resource filter for " + MultiBundleCSVFilter.ID, filter);

        MultiBundleResourceFilter multiFilter = ResourceFilterFactory.getMultiBundleResourceFilter(MultiBundleCSVFilter.ID);
        assertNotNull("Multi-bundle resource filter for " + MultiBundleCSVFilter.ID, multiFilter);
        assertEquals("Resource filter class", MultiBundleCSVFilter.class, multiFilter.getClass());
    }

    @Test
    public void testParse() {
        TestResourceStringData[] expectedFoods = {
                new TestResourceStringData("apple", "Apple", 1),
                new TestResourceStringData("orange", "Orange", 2)
        };

        TestResourceStringData[] expectedGreetings = {
                new TestResourceStringData("msg_hello", "Hello", 1),
                new TestResourceStringData("msg_bye", "Bye", 2)
        };

        Map<String, TestResourceStringData[]> expectedDataMap = new HashMap<>();
        expectedDataMap.put("foods", expectedFoods);
        expectedDataMap.put("greetings", expectedGreetings);

        MultiBundleResourceFilter filter = ResourceFilterFactory.getMultiBundleResourceFilter(MultiBundleCSVFilter.ID);
        try (InputStream inStream = this.getClass().getResourceAsStream("/test-multi.csv")) {
            Map<String, LanguageBundle> bundles = filter.parse(inStream, new FilterOptions(Locale.ENGLISH));

            assertEquals("Number of modules", expectedDataMap.size(), bundles.size());

            for (Entry<String, LanguageBundle> bundleEntry : bundles.entrySet()) {
                String module = bundleEntry.getKey();
                LanguageBundle bundle = bundleEntry.getValue();

                TestResourceStringData[] expStrings = expectedDataMap.get(module);
                assertNotNull("Module " + module, expStrings);

                List<ResourceString> resStrings = bundle.getSortedResourceStrings();
                assertEquals("Number of resource strings for module " + module, expStrings.length, resStrings.size());

                int idx = 0;
                for (ResourceString resString : resStrings) {
                    String key = resString.getKey();
                    String value = resString.getValue();
                    int seqNum = resString.getSequenceNumber();

                    TestResourceStringData expected = expStrings[idx++];

                    assertEquals("Resource key in module (" + module + ") at index " + idx, expected.key, key);
                    assertEquals("Resource value in module(" + module + ") at index " + idx, expected.value, value);
                    assertEquals("Resource sequence number in module(" + module + ") at index " + idx, expected.seq, seqNum);
                }
            }
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (ResourceFilterException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testWrite() {
        TestResourceStringData[] testDataJapanese = {
                new TestResourceStringData("sushi", "Sushi", 1),
                new TestResourceStringData("ramen", "Ramen", 2)
        };
        TestResourceStringData[] testDataItalian = {
                new TestResourceStringData("minestrone", "Minestrone", 2),
                new TestResourceStringData("pizza", "Pizza", 3),
                new TestResourceStringData("spaghetti", "Spaghetti", 1)
        };

        String[] expectedLines = {
                "module,key,value",
                "Italian Foods,spaghetti,Spaghetti",
                "Italian Foods,minestrone,Minestrone",
                "Italian Foods,pizza,Pizza",
                "Japanese Foods,sushi,Sushi",
                "Japanese Foods,ramen,Ramen"
        };

        Map<String, LanguageBundle> testDataMap = new LinkedHashMap<>();
        testDataMap.put("Japanese Foods", TestUtils.createLanguageBundle(testDataJapanese));
        testDataMap.put("Italian Foods", TestUtils.createLanguageBundle(testDataItalian));

        MultiBundleResourceFilter filter = ResourceFilterFactory.getMultiBundleResourceFilter(MultiBundleCSVFilter.ID);
        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            filter.write(outStream, testDataMap, new FilterOptions(Locale.ENGLISH));
            TestUtils.compareLines(expectedLines, outStream.toByteArray());
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (ResourceFilterException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testMerge() {
        String[] baseLines = {
                "module,key,value",
                "Japanese Foods,ramen,Ramen",
                "Italian Foods,minestrone,Minestrone",
                "Japanese Foods,sushi,Sushi",
                "Italian Foods,pizza,Pizza",
                "Italian Foods,spaghetti,Spaghetti"
        };

        TestResourceStringData[] testDataJapanese = {
                new TestResourceStringData("sushi", "寿司", 1),
                new TestResourceStringData("ramen", "ラーメン", 2),
                new TestResourceStringData("tempura", "天ぷら", 3)
        };
        TestResourceStringData[] testDataItalian = {
                new TestResourceStringData("spaghetti", "スパゲッティ", 1),
                new TestResourceStringData("pizza", "ピザ", 2)
        };

        String[] expectedLines = {
                "module,key,value",
                "Japanese Foods,ramen,ラーメン",
                "Italian Foods,minestrone,Minestrone",
                "Japanese Foods,sushi,寿司",
                "Italian Foods,pizza,ピザ",
                "Italian Foods,spaghetti,スパゲッティ"
        };

        Map<String, LanguageBundle> testDataMap = new LinkedHashMap<>();
        testDataMap.put("Japanese Foods", TestUtils.createLanguageBundle(testDataJapanese));
        testDataMap.put("Italian Foods", TestUtils.createLanguageBundle(testDataItalian));

        MultiBundleResourceFilter filter = ResourceFilterFactory.getMultiBundleResourceFilter(MultiBundleCSVFilter.ID);
        try (InputStream baseStream = TestUtils.creteInputStream(baseLines);
                ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            filter.merge(baseStream, outStream, testDataMap, new FilterOptions(Locale.JAPANESE));
            TestUtils.compareLines(expectedLines, outStream.toByteArray());
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (ResourceFilterException e) {
            fail(e.getMessage());
        }
    }
}
