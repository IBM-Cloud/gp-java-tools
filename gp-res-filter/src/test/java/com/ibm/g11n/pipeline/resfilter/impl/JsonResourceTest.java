/*
 * Copyright IBM Corp. 2016, 2018
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
package com.ibm.g11n.pipeline.resfilter.impl;

import static org.junit.Assert.assertArrayEquals;
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

import org.junit.Test;

import com.ibm.g11n.pipeline.resfilter.LanguageBundle;
import com.ibm.g11n.pipeline.resfilter.LanguageBundleBuilder;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterException;
import com.ibm.g11n.pipeline.resfilter.ResourceString;
import com.ibm.g11n.pipeline.resfilter.ResourceString.ResourceStringComparator;

/**
 * @author farhan
 *
 */
public class JsonResourceTest {
    private static final File INPUT_FILE = new File("src/test/resource/resfilter/json/input.json");
    private static final File INPUT_FILE2 = new File("src/test/resource/resfilter/json/other-input.json");

    private static final File EXPECTED_WRITE_FILE = new File("src/test/resource/resfilter/json/write-output.json");

    private static final Collection<ResourceString> EXPECTED_INPUT_RES_LIST;

    static {
        List<ResourceString> lst = new LinkedList<>();
        lst.add(ResourceString.with("$.bears.grizzly.brown", "Brown Bear").sequenceNumber(1).build());
        lst.add(ResourceString.with("$.bears.grizzly.black", "Black Bear").sequenceNumber(2).build());
        lst.add(ResourceString.with("$.bears.white", "Polar Bear").sequenceNumber(3).build());
        lst.add(ResourceString.with("$.countries[0].Europe[0]", "Germany").sequenceNumber(4).build());
        lst.add(ResourceString.with("$.countries[0].Europe[1]", "Italy").sequenceNumber(5).build());
        lst.add(ResourceString.with("$.countries[0].Europe[2]", "France").sequenceNumber(6).build());
        lst.add(ResourceString.with("$.countries[0].Europe[3]", "Spain").sequenceNumber(7).build());
        lst.add(ResourceString.with("$.countries[1].Asia[0]", "China").sequenceNumber(8).build());
        lst.add(ResourceString.with("$.countries[1].Asia[1]", "Japan").sequenceNumber(9).build());
        lst.add(ResourceString.with("$.countries[1].Asia[2]", "India").sequenceNumber(10).build());
        lst.add(ResourceString.with("$.countries[2].Americas['S. America'][0]", "Brazil").sequenceNumber(11).build());
        lst.add(ResourceString.with("$.countries[2].Americas['S. America'][1]", "Venezuela").sequenceNumber(12)
                .build());
        lst.add(ResourceString.with("$.countries[2].Americas['N. America'][0]", "United States [USA]")
                .sequenceNumber(13).build());
        lst.add(ResourceString.with("$.countries[2].Americas['N. America'][1]", "Canada").sequenceNumber(14).build());
        lst.add(ResourceString.with("$.countries[2].Americas['N. America'][2]", "Mexico").sequenceNumber(15).build());
        lst.add(ResourceString.with("$.countries[3].Africa[0]", "Egypt").sequenceNumber(16).build());
        lst.add(ResourceString.with("$.countries[3].Africa[1]", "Somalia").sequenceNumber(17).build());
        lst.add(ResourceString.with("$.countries[3].Africa[2]", "S. Africa").sequenceNumber(18).build());
        lst.add(ResourceString.with("$.colors[0]", "red").sequenceNumber(19).build());
        lst.add(ResourceString.with("$.colors[1]", "blue").sequenceNumber(20).build());
        lst.add(ResourceString.with("$.colors[2]", "yellow").sequenceNumber(21).build());
        lst.add(ResourceString.with("$.colors[3]", "orange").sequenceNumber(22).build());
        lst.add(ResourceString.with("some_text", "Just a plain old string").sequenceNumber(23).build());
        lst.add(ResourceString.with("another.text", "Another plain old string").sequenceNumber(24).build());
        lst.add(ResourceString.with("frog['2']", "Red-eyed Tree Frog").sequenceNumber(25).build());
        lst.add(ResourceString.with("owl[3]", "Great Horned Owl").sequenceNumber(26).build());
        lst.add(ResourceString.with("$['$.xxx']", "Looks like JSONPATH, but actually plain old string")
                .sequenceNumber(27).build());
        lst.add(ResourceString.with("$['$.']", "Looks like JSONPATH prefix, but actually plain old string")
                .sequenceNumber(28).build());
        lst.add(ResourceString.with("$abc", "Starts with JSONPATH root char, but just a string").sequenceNumber(29)
                .build());
        lst.add(ResourceString.with("$['ibm.com']['g11n.pipeline.title']", "Globalization Pipeline").sequenceNumber(30)
                .build());

        Collections.sort(lst, new ResourceStringComparator());
        EXPECTED_INPUT_RES_LIST = lst;
    }

    private static LanguageBundle WRITE_BUNDLE;

    static {
        LanguageBundleBuilder bundleBuilder = new LanguageBundleBuilder(false);
        bundleBuilder.addResourceString("$.bears.grizzly.brown", "Brown Bear - XL", 1);
        bundleBuilder.addResourceString("$.bears.grizzly.black", "Black Bear - XL", 2);
        bundleBuilder.addResourceString("$.bears.white", "Polar Bear - XL", 3);
        bundleBuilder.addResourceString("$.countries[0].Europe[0]", "Germany - XL", 4);
        bundleBuilder.addResourceString("$.countries[0].Europe[1]", "Italy - XL", 5);
        bundleBuilder.addResourceString("$.countries[0].Europe[2]", "France - XL", 6);
        bundleBuilder.addResourceString("$.countries[0].Europe[3]", "Spain - XL", 7);
        bundleBuilder.addResourceString("$.countries[1].Asia[0]", "China - XL", 8);
        bundleBuilder.addResourceString("$.countries[1].Asia[1]", "Japan - XL", 9);
        bundleBuilder.addResourceString("$.countries[1].Asia[2]", "India - XL", 10);
        bundleBuilder.addResourceString("$.countries[2].Americas['S. America'][0]", "Brazil - XL", 11);
        bundleBuilder.addResourceString("$.countries[2].Americas['S. America'][1]", "Venezuela - XL", 12);
        bundleBuilder.addResourceString("$.countries[2].Americas['N. America'][0]", "United States [USA] - XL", 13);
        bundleBuilder.addResourceString("$.countries[2].Americas['N. America'][1]", "Canada - XL", 14);
        bundleBuilder.addResourceString("$.countries[2].Americas['N. America'][2]", "Mexico - XL", 15);
        bundleBuilder.addResourceString("$.countries[3].Africa[0]", "Egypt - XL", 16);
        bundleBuilder.addResourceString("$.countries[3].Africa[1]", "Somalia - XL", 17);
        bundleBuilder.addResourceString("$.countries[3].Africa[2]", "S. Africa - XL", 18);
        bundleBuilder.addResourceString("$.colors[0]", "red - XL", 19);
        bundleBuilder.addResourceString("$.colors[1]", "blue - XL", 20);
        bundleBuilder.addResourceString("$.colors[2]", "yellow - XL", 21);
        bundleBuilder.addResourceString("$.colors[3]", "orange - XL", 22);
        bundleBuilder.addResourceString("some_text", "Just a plain old string - XL", 23);
        bundleBuilder.addResourceString("another.text", "Another plain old string - XL", 24);
        bundleBuilder.addResourceString("frog['2']", "Red-eyed Tree Frog - XL", 25);
        bundleBuilder.addResourceString("owl[3]", "Great Horned Owl - XL", 26);
        bundleBuilder.addResourceString("$['$.xxx']", "Looks like JSONPATH, but actually plain old string - XL", 27);
        bundleBuilder.addResourceString("$['$.']", "Looks like JSONPATH prefix, but actually plain old string - XL",
                28);
        bundleBuilder.addResourceString("$abc", "Starts with JSONPATH root char, but just a string - XL", 29);
        bundleBuilder.addResourceString("$['ibm.com']['g11n.pipeline.title']", "Globalization Pipeline - XL", 30);
        WRITE_BUNDLE = bundleBuilder.build();
    }

    private static final JsonResource res = new JsonResource();

    @Test
    public void testParse() throws IOException, ResourceFilterException {
        assertTrue("The input test file <" + INPUT_FILE + "> does not exist.", INPUT_FILE.exists());

        try (InputStream is = new FileInputStream(INPUT_FILE)) {
            LanguageBundle bundle = res.parse(is, null);
            List<ResourceString> resStrList = new ArrayList<>(bundle.getResourceStrings());
            Collections.sort(resStrList, new ResourceStringComparator());
            assertArrayEquals("ResourceStrings did not match.", EXPECTED_INPUT_RES_LIST.toArray(),
                    resStrList.toArray());
        }
    }

    @Test
    public void testWrite() throws IOException, ResourceFilterException {
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".json");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile)) {
            res.write(os, WRITE_BUNDLE, null);
            os.flush();
            ResourceTestUtil.compareFilesJson(EXPECTED_WRITE_FILE, tempFile);
        }
    }

    // @Test
    // public void testTestFiles() throws IOException, ResourceFilterException {
    // // just test the test files
    // ResourceTestUtil.compareFilesJson(INPUT_FILE, EXPECTED_WRITE_FILE);
    // }

    @Test
    public void testReWrite() throws IOException, ResourceFilterException {
        // First parse
        assertTrue("The input test file <" + INPUT_FILE + "> does not exist.", INPUT_FILE.exists());

        try (InputStream is = new FileInputStream(INPUT_FILE)) {
            JsonResource res2 = new JsonResource();
            LanguageBundle bundle = res2.parse(is, null);
            List<ResourceString> resStrList = new ArrayList<>(bundle.getResourceStrings());
            Collections.sort(resStrList, new ResourceStringComparator());
            assertArrayEquals("ResourceStrings did not match.", EXPECTED_INPUT_RES_LIST.toArray(),
                    resStrList.toArray());

            // Now write
            File tempFile = File.createTempFile(this.getClass().getSimpleName(), "2.json");
            // File tempFile = new File("/tmp/2.json");
            tempFile.deleteOnExit();

            try (OutputStream os = new FileOutputStream(tempFile)) {
                res.write(os, bundle, null);
                os.flush();
                ResourceTestUtil.compareFilesJson(INPUT_FILE, tempFile);
            }
        }
    }

    @Test
    public void testReWriteOther() throws IOException, ResourceFilterException {
        // First parse
        assertTrue("The input test file <" + INPUT_FILE + "> does not exist.", INPUT_FILE2.exists());

        try (InputStream is = new FileInputStream(INPUT_FILE2)) {
            JsonResource res2 = new JsonResource();
            LanguageBundle bundle = res2.parse(is, null);
            List<ResourceString> resStrList = new ArrayList<>(bundle.getResourceStrings());
            Collections.sort(resStrList, new ResourceStringComparator());
            // assertEquals("ResourceStrings did not match.",
            // EXPECTED_INPUT_RES_LIST, resStrList);

            // Now write
            File tempFile = File.createTempFile(this.getClass().getSimpleName(), "3.json");
            // File tempFile = new File("/tmp/3.json");
            tempFile.deleteOnExit();

            try (OutputStream os = new FileOutputStream(tempFile)) {
                res.write(os, bundle, null);
                os.flush();
                System.out.println(ResourceTestUtil.fileToString(tempFile));
                ResourceTestUtil.compareFilesJson(INPUT_FILE2, tempFile);
            }
        }
    }

    // TODO: Not ready yet
    // @Test
    // public void testMerge() throws IOException, ResourceFilterException {
    // }
}
