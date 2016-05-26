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
public class AndroidStringsResourceTest {
    private static final File INPUT_FILE = new File("src/test/resource/resfilter/xml/input.xml");

    private static final File EXPECTED_WRITE_FILE = new File("src/test/resource/resfilter/xml/write-output.xml");

    private static Collection<ResourceString> EXPECTED_INPUT_RES_LIST;

    static {
        EXPECTED_INPUT_RES_LIST = new LinkedList<ResourceString>();
        EXPECTED_INPUT_RES_LIST.add(new ResourceString("planets_array", "[Mercury, Venus, Earth, Mars]", 1));
        EXPECTED_INPUT_RES_LIST.add(new ResourceString("bear", "Brown Bear", 2));
        EXPECTED_INPUT_RES_LIST.add(new ResourceString("frog", "Red-eyed Tree Frog", 3));
        EXPECTED_INPUT_RES_LIST.add(new ResourceString("owl", "Great Horned Owl", 4));
    }

    private static Collection<ResourceString> WRITE_RES_LIST;

    static {
        WRITE_RES_LIST = new LinkedList<ResourceString>();
        WRITE_RES_LIST.add(new ResourceString("sealion3", "California Sea Lion", 3));
        WRITE_RES_LIST.add(new ResourceString("otter1", "Sea Otter", 1));
        WRITE_RES_LIST.add(new ResourceString("crow2", "American Crow", 2));
        WRITE_RES_LIST.add(new ResourceString("planets_array4", "[Mercury, Venus, Earth, Mars]", 4));
    }

    private static final AndroidStringsResource res = new AndroidStringsResource();

    @Test
    public void testParse() throws IOException {
        assertTrue("The input test file <" + INPUT_FILE + "> does not exist.", INPUT_FILE.exists());

        try (InputStream is = new FileInputStream(INPUT_FILE)) {
            Collection<ResourceString> resStrs = res.parse(is);
            System.out.println(resStrs);
            assertEquals("ResourceStrings did not match.", EXPECTED_INPUT_RES_LIST, resStrs);
        }
    }

    @Test
    public void testWrite() throws IOException {
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".xml");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile)) {
            res.write(os, null, WRITE_RES_LIST);
            os.flush();
            // TODO: It looks IBM and Oracle/OpenJDK produces different XML output,
            // so byte-to-byte comparison fails depending on Java runtime.
//            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_WRITE_FILE, tempFile));
        }
    }

    @Ignore("not ready yet")
    @Test
    public void testMerge() throws IOException {
    }
}