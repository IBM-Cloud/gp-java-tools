/*
 * Copyright IBM Corp. 2016
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
import java.util.Collection;
import java.util.LinkedList;

import org.junit.Test;

/**
 * @author farhan
 *
 */
public class JsonResourceTest {
    private static final File INPUT_FILE = new File("src/test/resource/resfilter/json/input.json");

    private static final File EXPECTED_WRITE_FILE = new File("src/test/resource/resfilter/json/write-output.json");

    private static final File EXPECTED_MERGE_FILE = new File("src/test/resource/resfilter/json/merge-output.json");

    private static Collection<ResourceString> EXPECTED_INPUT_RES_LIST;

    static {
        EXPECTED_INPUT_RES_LIST = new LinkedList<ResourceString>();
        EXPECTED_INPUT_RES_LIST.add(new ResourceString("bear 1", "Brown Bear", 1));
        EXPECTED_INPUT_RES_LIST.add(new ResourceString("frog 2", "Red-eyed Tree Frog", 2));
        EXPECTED_INPUT_RES_LIST.add(new ResourceString("owl 3", "Great Horned Owl", 3));
    }

    private static Collection<ResourceString> WRITE_RES_LIST;

    static {
        WRITE_RES_LIST = new LinkedList<ResourceString>();
        WRITE_RES_LIST.add(new ResourceString("owl 3", "Great Horned Owl - translated", 3));
        WRITE_RES_LIST.add(new ResourceString("bear 1", "Brown Bear - translated", 1));
        WRITE_RES_LIST.add(new ResourceString("frog 2", "Red-eyed Tree Frog - translated", 2));
    }

    private static final JsonResource res = new JsonResource();

    @Test
    public void testParse() throws IOException {
        assertTrue("The input test file <" + INPUT_FILE + "> does not exist.", INPUT_FILE.exists());

        try (InputStream is = new FileInputStream(INPUT_FILE)) {
            Collection<ResourceString> resStrs = res.parse(is);
            assertEquals("ResourceStrings did not match.", EXPECTED_INPUT_RES_LIST, resStrs);
        }
    }

    @Test
    public void testWrite() throws IOException {
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".json");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile)) {
            res.write(os, null, WRITE_RES_LIST);
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_WRITE_FILE, tempFile));
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testMerge() throws IOException {
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".json");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile); InputStream is = new FileInputStream(INPUT_FILE)) {
            res.merge(is, os, null, WRITE_RES_LIST);
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_MERGE_FILE, tempFile));
        }
    }
}
