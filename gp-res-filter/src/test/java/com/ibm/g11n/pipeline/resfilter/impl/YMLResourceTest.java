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

import com.ibm.g11n.pipeline.resfilter.LanguageBundle;
import com.ibm.g11n.pipeline.resfilter.LanguageBundleBuilder;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterException;
import com.ibm.g11n.pipeline.resfilter.ResourceString;

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
        EXPECTED_INPUT_RES_LIST.add(ResourceString.with(key, key).sequenceNumber(1).build());

    }

    private static LanguageBundle WRITE_BUNDLE;

    static {
        LanguageBundleBuilder bundleBuilder = new LanguageBundleBuilder(false);

        String key, value;

        key = "Untranslated Sea Lion 3";
        value = "TRANSLATED: California Sea Lion";
        bundleBuilder.addResourceString(key, value, 3);
        WRITE_BUNDLE = bundleBuilder.build();
    }

    private static final YMLResource res = new YMLResource();

    @Test
    public void testParse() throws IOException, ResourceFilterException {
        assertTrue("The input test file <" + INPUT_FILE + "> does not exist.", INPUT_FILE.exists());

        try (InputStream is = new FileInputStream(INPUT_FILE)) {
            @SuppressWarnings("unused")
            LanguageBundle bundle =  res.parse(is, null);
            // TODO: Not ready yet
            // assertEquals("ResourceStrings did not match.", EXPECTED_INPUT_RES_LIST, bundle.getResourceStrings());
        }
    }

    @Test
    public void testWrite() throws IOException, ResourceFilterException {
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".yml");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile)) {
            res.write(os, WRITE_BUNDLE, null);
            os.flush();
            // TODO: Not ready yet
            // assertTrue(ResourceTestUtil.compareFiles(EXPECTED_WRITE_FILE, tempFile));
        }
    }

    @Test
    public void testMerge() throws IOException, ResourceFilterException {
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".yml");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile); InputStream is = new FileInputStream(INPUT_FILE)) {
            res.merge(is, os, WRITE_BUNDLE, null);
            os.flush();
            // TODO: Not ready yet
            // assertTrue(ResourceTestUtil.compareFiles(EXPECTED_MERGE_FILE, tempFile));
        }
    }
}
