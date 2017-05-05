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
public class GlobalizeJsResourceTest {
    private static final File INPUT_FILE = new File("src/test/resource/resfilter/globalizejs/input.json");

    private static final File EXPECTED_WRITE_FILE = new File("src/test/resource/resfilter/globalizejs/write-output.json");

    private static final Collection<ResourceString> EXPECTED_INPUT_RES_LIST;
    private static final String LONG_TEXT_STRING = 
        "This is a really long sentence which " +
        "is supposed to be joined together with " +
        "a space between each string, " +
        "or at least that is what the documentation says.";

    private static final String LONG_TEXT_STRING_DE =
        "Dies ist ein wirklich langer Satz, " +
        "der zusammen mit einem Leerzeichen zwischen jedem String verbunden sein soll, " +
        "oder zumindest das ist, was die Dokumentation sagt.";

    static {
        List<ResourceString> lst = new LinkedList<>();
        lst.add(new ResourceString("Action", "Action", 1));
        lst.add(new ResourceString("File", "File", 2));
        lst.add(new ResourceString("longText", LONG_TEXT_STRING, 3));
        lst.add(new ResourceString("$.Colors.Red", "R", 4));
        lst.add(new ResourceString("$.Colors.Green", "G", 5));
        lst.add(new ResourceString("$.Colors.Blue", "B", 6));
        lst.add(new ResourceString("Done", "Finish", 7));

        Collections.sort(lst, new ResourceStringComparator());
        EXPECTED_INPUT_RES_LIST = lst;
    }

    private static Bundle WRITE_BUNDLE;

    static {
        WRITE_BUNDLE = new Bundle();
        WRITE_BUNDLE.addResourceString("Action", "Aktion", 1);
        WRITE_BUNDLE.addResourceString("File", "Datei", 2);
        WRITE_BUNDLE.addResourceString("longText", LONG_TEXT_STRING_DE, 3);
        WRITE_BUNDLE.addResourceString("$.Colors.Red", "Rot", 4);
        WRITE_BUNDLE.addResourceString("$.Colors.Green", "Gr\u00FCn", 5);
        WRITE_BUNDLE.addResourceString("$.Colors.Blue", "Blau", 6);
        WRITE_BUNDLE.addResourceString("Done", "Fertig", 7);
    }

    private static final GlobalizeJsResource res = new GlobalizeJsResource();

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
            res.write(os, "de", WRITE_BUNDLE);
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
