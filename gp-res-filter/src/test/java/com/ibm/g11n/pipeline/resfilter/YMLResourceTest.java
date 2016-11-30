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

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author farhan
 *
 */
public class YMLResourceTest {
    private static final File INPUT_FILE = new File("src/test/resource/resfilter/yml/input.yml");

    @SuppressWarnings("unused")
    private static final File EXPECTED_WRITE_FILE = new File("src/test/resource/resfilter/yml/write-output.yml");

    @SuppressWarnings("unused")
    private static final File EXPECTED_MERGE_FILE = new File("src/test/resource/resfilter/yml/merge-output.yml");

    private static Collection<ResourceString> EXPECTED_INPUT_RES_LIST;

    static {
        EXPECTED_INPUT_RES_LIST = new LinkedList<ResourceString>();

        String key;

        key = "untranslated-string";
        EXPECTED_INPUT_RES_LIST.add(new ResourceString(key, key, 1));

    }

    private static Bundle WRITE_BUNDLE;

    static {
        WRITE_BUNDLE = new Bundle();

        String key, value;

        key = "Untranslated Sea Lion 3";
        value = "TRANSLATED: California Sea Lion";
        WRITE_BUNDLE.addResourceString(key, value, 3);
    }

    private static final YMLResource res = new YMLResource();

    @Test
    public void testParse() throws IOException {
        assertTrue("The input test file <" + INPUT_FILE + "> does not exist.", INPUT_FILE.exists());

        try (InputStream is = new FileInputStream(INPUT_FILE)) {
            @SuppressWarnings("unused")
            Bundle bundle =  res.parse(is);
            // TODO: Not ready yet
            // assertEquals("ResourceStrings did not match.", EXPECTED_INPUT_RES_LIST, bundle.getResourceStrings());
        }
    }

    @Test
    public void testWrite() throws IOException {
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".yml");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile)) {
            res.write(os, null, WRITE_BUNDLE);
            os.flush();
            // TODO: Not ready yet
            // assertTrue(ResourceTestUtil.compareFiles(EXPECTED_WRITE_FILE, tempFile));
        }
    }

    @Test
    public void testMerge() throws IOException {
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".yml");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile); InputStream is = new FileInputStream(INPUT_FILE)) {
            res.merge(is, os, null, WRITE_BUNDLE);
            os.flush();
            // TODO: Not ready yet
            // assertTrue(ResourceTestUtil.compareFiles(EXPECTED_MERGE_FILE, tempFile));
        }
    }
}
