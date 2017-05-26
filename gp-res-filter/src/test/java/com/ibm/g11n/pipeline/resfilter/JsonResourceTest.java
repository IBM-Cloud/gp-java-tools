/*
 * Copyright IBM Corp. 2016, 2017
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

import static org.junit.Assert.assertEquals;
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

import com.ibm.g11n.pipeline.resfilter.ResourceString.ResourceStringComparator;

/**
 * @author farhan
 *
 */
public class JsonResourceTest {
    private static final File INPUT_FILE = new File("src/test/resource/resfilter/json/input.json");

    private static final File EXPECTED_WRITE_FILE = new File("src/test/resource/resfilter/json/write-output.json");

    private static final Collection<ResourceString> EXPECTED_INPUT_RES_LIST;

    static {
        List<ResourceString> lst = new LinkedList<>();
        lst.add(new ResourceString("$.bears.grizzly.brown", "Brown Bear", 1));
        lst.add(new ResourceString("$.bears.grizzly.black", "Black Bear", 2));
        lst.add(new ResourceString("$.bears.white", "Polar Bear", 3));
        lst.add(new ResourceString("$.countries[0].Europe[0]", "Germany", 4));
        lst.add(new ResourceString("$.countries[0].Europe[1]", "Italy", 5));
        lst.add(new ResourceString("$.countries[0].Europe[2]", "France", 6));
        lst.add(new ResourceString("$.countries[0].Europe[3]", "Spain", 7));
        lst.add(new ResourceString("$.countries[1].Asia[0]", "China", 8));
        lst.add(new ResourceString("$.countries[1].Asia[1]", "Japan", 9));
        lst.add(new ResourceString("$.countries[1].Asia[2]", "India", 10));
        lst.add(new ResourceString("$.countries[2].Americas['S. America'][0]", "Brazil", 11));
        lst.add(new ResourceString("$.countries[2].Americas['S. America'][1]", "Venezuela", 12));
        lst.add(new ResourceString("$.countries[2].Americas['N. America'][0]", "United States [USA]", 13));
        lst.add(new ResourceString("$.countries[2].Americas['N. America'][1]", "Canada", 14));
        lst.add(new ResourceString("$.countries[2].Americas['N. America'][2]", "Mexico", 15));
        lst.add(new ResourceString("$.countries[3].Africa[0]", "Egypt", 16));
        lst.add(new ResourceString("$.countries[3].Africa[1]", "Somalia", 17));
        lst.add(new ResourceString("$.countries[3].Africa[2]", "S. Africa", 18));
        lst.add(new ResourceString("$.colors[0]", "red", 19));
        lst.add(new ResourceString("$.colors[1]", "blue", 20));
        lst.add(new ResourceString("$.colors[2]", "yellow", 21));
        lst.add(new ResourceString("$.colors[3]", "orange", 22));
        lst.add(new ResourceString("some_text", "Just a plain old string", 23));
        lst.add(new ResourceString("another.text", "Another plain old string", 24));
        lst.add(new ResourceString("frog['2']", "Red-eyed Tree Frog", 25));
        lst.add(new ResourceString("owl[3]", "Great Horned Owl", 26));
        lst.add(new ResourceString("$['$.xxx']", "Looks like JSONPATH, but actually plain old string", 27));
        lst.add(new ResourceString("$['$.']", "Looks like JSONPATH prefix, but actually plain old string", 28));
        lst.add(new ResourceString("$abc", "Starts with JSONPATH root char, but just a string", 29));
        lst.add(new ResourceString("$['ibm.com']['g11n.pipeline.title']", "Globalization Pipeline", 30));

        Collections.sort(lst, new ResourceStringComparator());
        EXPECTED_INPUT_RES_LIST = lst;
    }

    private static Bundle WRITE_BUNDLE;

    static {
        WRITE_BUNDLE = new Bundle();
        WRITE_BUNDLE.addResourceString("$.bears.grizzly.brown", "Brown Bear - XL", 1);
        WRITE_BUNDLE.addResourceString("$.bears.grizzly.black", "Black Bear - XL", 2);
        WRITE_BUNDLE.addResourceString("$.bears.white", "Polar Bear - XL", 3);
        WRITE_BUNDLE.addResourceString("$.countries[0].Europe[0]", "Germany - XL", 4);
        WRITE_BUNDLE.addResourceString("$.countries[0].Europe[1]", "Italy - XL", 5);
        WRITE_BUNDLE.addResourceString("$.countries[0].Europe[2]", "France - XL", 6);
        WRITE_BUNDLE.addResourceString("$.countries[0].Europe[3]", "Spain - XL", 7);
        WRITE_BUNDLE.addResourceString("$.countries[1].Asia[0]", "China - XL", 8);
        WRITE_BUNDLE.addResourceString("$.countries[1].Asia[1]", "Japan - XL", 9);
        WRITE_BUNDLE.addResourceString("$.countries[1].Asia[2]", "India - XL", 10);
        WRITE_BUNDLE.addResourceString("$.countries[2].Americas['S. America'][0]", "Brazil - XL", 11);
        WRITE_BUNDLE.addResourceString("$.countries[2].Americas['S. America'][1]", "Venezuela - XL", 12);
        WRITE_BUNDLE.addResourceString("$.countries[2].Americas['N. America'][0]", "United States [USA] - XL", 13);
        WRITE_BUNDLE.addResourceString("$.countries[2].Americas['N. America'][1]", "Canada - XL", 14);
        WRITE_BUNDLE.addResourceString("$.countries[2].Americas['N. America'][2]", "Mexico - XL", 15);
        WRITE_BUNDLE.addResourceString("$.countries[3].Africa[0]", "Egypt - XL", 16);
        WRITE_BUNDLE.addResourceString("$.countries[3].Africa[1]", "Somalia - XL", 17);
        WRITE_BUNDLE.addResourceString("$.countries[3].Africa[2]", "S. Africa - XL", 18);
        WRITE_BUNDLE.addResourceString("$.colors[0]", "red - XL", 19);
        WRITE_BUNDLE.addResourceString("$.colors[1]", "blue - XL", 20);
        WRITE_BUNDLE.addResourceString("$.colors[2]", "yellow - XL", 21);
        WRITE_BUNDLE.addResourceString("$.colors[3]", "orange - XL", 22);
        WRITE_BUNDLE.addResourceString("some_text", "Just a plain old string - XL", 23);
        WRITE_BUNDLE.addResourceString("another.text", "Another plain old string - XL", 24);
        WRITE_BUNDLE.addResourceString("frog['2']", "Red-eyed Tree Frog - XL", 25);
        WRITE_BUNDLE.addResourceString("owl[3]", "Great Horned Owl - XL", 26);
        WRITE_BUNDLE.addResourceString("$['$.xxx']", "Looks like JSONPATH, but actually plain old string - XL", 27);
        WRITE_BUNDLE.addResourceString("$['$.']", "Looks like JSONPATH prefix, but actually plain old string - XL", 28);
        WRITE_BUNDLE.addResourceString("$abc", "Starts with JSONPATH root char, but just a string - XL", 29);
        WRITE_BUNDLE.addResourceString("$['ibm.com']['g11n.pipeline.title']", "Globalization Pipeline - XL", 30);
    }

    private static final JsonResource res = new JsonResource();

    @Test
    public void testParse() throws IOException {
        assertTrue("The input test file <" + INPUT_FILE + "> does not exist.", INPUT_FILE.exists());

        try (InputStream is = new FileInputStream(INPUT_FILE)) {
            Bundle bundle = res.parse(is);
            List<ResourceString> resStrList = new ArrayList<>(bundle.getResourceStrings());
            Collections.sort(resStrList, new ResourceStringComparator());
            assertEquals("ResourceStrings did not match.", EXPECTED_INPUT_RES_LIST, resStrList);
        }
    }

    @Test
    public void testWrite() throws IOException {
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".json");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile)) {
            res.write(os, null, WRITE_BUNDLE);
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_WRITE_FILE, tempFile));
        }
    }

//    @Test
//    public void testMerge() throws IOException {
//        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".json");
//        tempFile.deleteOnExit();

//        try (OutputStream os = new FileOutputStream(tempFile); InputStream is = new FileInputStream(INPUT_FILE)) {
//            res.merge(is, os, null, WRITE_BUNDLE);
//            os.flush();
            // TODO: Not ready yet
            // assertTrue(ResourceTestUtil.compareFiles(EXPECTED_MERGE_FILE, tempFile));
//        }
//    }
}
