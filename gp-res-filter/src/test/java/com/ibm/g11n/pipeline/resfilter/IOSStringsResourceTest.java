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
public class IOSStringsResourceTest {
    private static final File INPUT = new File("src/test/resource/resfilter/ios/input.strings");

    private static final File EXPECTED_WRITE_FILE = new File("src/test/resource/resfilter/ios/write-output.strings");

    private static Collection<ResourceString> EXPECTED_INPUT_RES_LIST;

    static {
        EXPECTED_INPUT_RES_LIST = new LinkedList<ResourceString>();
        EXPECTED_INPUT_RES_LIST.add(new ResourceString("Insert Element", "Insert Element", 1));
        EXPECTED_INPUT_RES_LIST.add(new ResourceString("ErrorString_1", "An unknown error occurred.", 2));
        EXPECTED_INPUT_RES_LIST.add(new ResourceString("bear 3", "Brown Bear", 3));
        EXPECTED_INPUT_RES_LIST.add(new ResourceString("frog 4", "Red-eyed Tree Frog", 4));
        EXPECTED_INPUT_RES_LIST.add(new ResourceString("owl 5", "Great Horned Owl", 5));
    }

    private static Collection<ResourceString> WRITE_RES_LIST;

    static {
        WRITE_RES_LIST = new LinkedList<ResourceString>();
        WRITE_RES_LIST.add(new ResourceString("sealion 3", "California Sea Lion", 3));
        WRITE_RES_LIST.add(new ResourceString("otter 1", "Sea Otter", 1));
        WRITE_RES_LIST.add(new ResourceString("crow 2", "American Crow", 2));
    }

    private static final IOSStringsResource res = new IOSStringsResource();

    @Test
    public void testParse() throws IOException {
        assertTrue("The input test file <" + INPUT + "> does not exist.", INPUT.exists());

        try (InputStream is = new FileInputStream(INPUT)) {
            Collection<ResourceString> resStrs = res.parse(is);
            assertEquals("ResourceStrings did not match.", EXPECTED_INPUT_RES_LIST, resStrs);
        }
    }

    @Test
    public void testWrite() throws IOException {
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".strings");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile)) {
            res.write(os, null, WRITE_RES_LIST);
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_WRITE_FILE, tempFile));
        }
    }

    @Ignore("not ready yet")
    @Test
    public void testMerge() throws IOException {
    }
}
