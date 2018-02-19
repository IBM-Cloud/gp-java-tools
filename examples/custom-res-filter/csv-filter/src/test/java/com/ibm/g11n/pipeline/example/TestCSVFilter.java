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
package com.ibm.g11n.pipeline.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.Test;

import com.ibm.g11n.pipeline.resfilter.FilterOptions;
import com.ibm.g11n.pipeline.resfilter.LanguageBundle;
import com.ibm.g11n.pipeline.resfilter.MultiBundleResourceFilter;
import com.ibm.g11n.pipeline.resfilter.ResourceFilter;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterException;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterFactory;
import com.ibm.g11n.pipeline.resfilter.ResourceString;

public class TestCSVFilter {
    @Test
    public void testFactory() {
        Set<String> availableIDs = ResourceFilterFactory.getAvailableFilterIds();
        assertTrue("Available filter IDs contains " + CSVFilter.ID, availableIDs.contains(CSVFilter.ID));

        ResourceFilter filter = ResourceFilterFactory.getResourceFilter(CSVFilter.ID);
        assertNotNull("Resource filter for " + CSVFilter.ID, filter);
        assertEquals("Resource filter class", CSVFilter.class, filter.getClass());

        MultiBundleResourceFilter multiFilter = ResourceFilterFactory.getMultiBundleResourceFilter(CSVFilter.ID);
        assertNull("Multi-bundle resource filter for " + CSVFilter.ID, multiFilter);
    }


    @Test
    public void testParse() {
        TestResourceStringData[] expectedData = {
                new TestResourceStringData("msg_hello", "Hello", 1),
                new TestResourceStringData("msg_bye", "Bye", 2)
        };

        ResourceFilter filter = ResourceFilterFactory.getResourceFilter(CSVFilter.ID);
        try (InputStream inStream = this.getClass().getResourceAsStream("/test.csv")) {
            LanguageBundle bundle = filter.parse(inStream, new FilterOptions(Locale.ENGLISH));
            List<ResourceString> resStrings = bundle.getSortedResourceStrings();

            assertEquals("Number of resource strings", expectedData.length, resStrings.size());
            int idx = 0;
            for (ResourceString resString : resStrings) {
                String key = resString.getKey();
                String value = resString.getValue();
                int seqNum = resString.getSequenceNumber();

                TestResourceStringData expected = expectedData[idx++];

                assertEquals("Resource key at index " + idx, expected.key, key);
                assertEquals("Resource value at index " + idx, expected.value, value);
                assertEquals("Resource sequence number at index " + idx, expected.seq, seqNum);
            }
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (ResourceFilterException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testWrite() {
        TestResourceStringData[] testData = {
                new TestResourceStringData("minestrone", "Minestrone", 2),
                new TestResourceStringData("pizza", "Pizza", 3),
                new TestResourceStringData("spaghetti", "Spaghetti", 1)
        };

        String[] expectedLines = {
                "key,value",
                "spaghetti,Spaghetti",
                "minestrone,Minestrone",
                "pizza,Pizza"
        };

        LanguageBundle bundle = TestUtils.createLanguageBundle(testData);
        ResourceFilter filter = ResourceFilterFactory.getResourceFilter(CSVFilter.ID);
        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            filter.write(outStream, bundle, new FilterOptions(Locale.ENGLISH));
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
                "key,value",
                "spaghetti,Spaghetti",
                "minestrone,Minestrone",
                "pizza,Pizza"
        };

        TestResourceStringData[] testData = {
                new TestResourceStringData("pizza", "ピザ", 1),
                new TestResourceStringData("spaghetti", "スパゲッティ", 2),
                new TestResourceStringData("calzone", "カルゾーン", 3)
        };

        String[] expectedLines = {
                "key,value",
                "spaghetti,スパゲッティ",
                "minestrone,Minestrone",
                "pizza,ピザ"
        };

        LanguageBundle bundle = TestUtils.createLanguageBundle(testData);
        ResourceFilter filter = ResourceFilterFactory.getResourceFilter(CSVFilter.ID);
        try (InputStream baseStream = TestUtils.creteInputStream(baseLines);
                ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            filter.merge(baseStream, outStream, bundle, new FilterOptions(Locale.JAPANESE));
            TestUtils.compareLines(expectedLines, outStream.toByteArray());
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (ResourceFilterException e) {
            fail(e.getMessage());
        }
    }

}
