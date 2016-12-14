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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.ibm.g11n.pipeline.resfilter.JavaPropertiesResource.PropDef;
import com.ibm.g11n.pipeline.resfilter.JavaPropertiesResource.PropDef.PropSeparator;
import com.ibm.g11n.pipeline.resfilter.ResourceString.ResourceStringComparator;

/**
 * @author Farhan Arshad
 *
 */
public class JavaPropertiesResourceTest {
    private static final File INPUT_FILE = new File("src/test/resource/resfilter/properties/input.properties");

    private static final File EXPECTED_WRITE_FILE = new File(
            "src/test/resource/resfilter/properties/write-output.properties");

    private static final File EXPECTED_MERGE_FILE = new File(
            "src/test/resource/resfilter/properties/merge-output.properties");

    private static final File PARSE_TEST_INPUT_FILE = new File(
            "src/test/resource/resfilter/properties/parseline-test-input.properties");

    private static final Collection<ResourceString> EXPECTED_INPUT_RES_LIST;

    static {
        List<ResourceString> lst = new LinkedList<>();
        lst.add(new ResourceString("website", "http://en.wikipedia.org/", 1));
        lst.add(new ResourceString("language", "English", 2));
        List<String> resourceNotes = new ArrayList<>();
        resourceNotes.add(" The backslash below tells the application to continue reading");
        resourceNotes.add(" the value onto the next line.");
        lst.add(new ResourceString("message", "Welcome to Wikipedia!", 3, resourceNotes));
        resourceNotes.clear();
        resourceNotes.add(" Add spaces to the key");
        lst.add(new ResourceString("key with spaces",
                "This is the value that could be looked up with the key \"key with spaces\".", 4, resourceNotes));

        ResourceString rs5 = new ResourceString("tab", "pick up the\u00A5 tab", 5);
        rs5.addNote(" Unicode");
        lst.add(rs5);
        Collections.sort(lst, new ResourceStringComparator());
        EXPECTED_INPUT_RES_LIST = lst;
    }

    private static Bundle WRITE_BUNDLE;

    static {
        WRITE_BUNDLE = new Bundle();
        WRITE_BUNDLE.addResourceString("language", "Not-English", 2);
        WRITE_BUNDLE.addResourceString("key with spaces",
                "Translated - This is the value that could be looked up with the key \"key with spaces\".", 4);
        WRITE_BUNDLE.addResourceString("website", "http://en.wikipedia.org/translated", 1);
        WRITE_BUNDLE.addResourceString("message", "Translated - Welcome to Wikipedia!", 3);
        WRITE_BUNDLE.addResourceString("tab", "Translated - pick up the\u00A5 tab", 5);
    }

    private static LinkedList<PropDef> EXPECTED_PROP_DEF_LIST;

    static {
        EXPECTED_PROP_DEF_LIST = new LinkedList<PropDef>();
        EXPECTED_PROP_DEF_LIST.add(new PropDef("website", "http://en.wikipedia.org/", PropSeparator.EQUAL));
        EXPECTED_PROP_DEF_LIST.add(new PropDef("language", "English", PropSeparator.SPACE));
        EXPECTED_PROP_DEF_LIST.add(new PropDef("message", "Welcome to Wikipedia!", PropSeparator.COLON));
        EXPECTED_PROP_DEF_LIST.add(new PropDef("key with spaces",
                "This is the value that could be looked up with the key \"key with spaces\".", PropSeparator.EQUAL));
        EXPECTED_PROP_DEF_LIST.add(new PropDef("tab", "pick up the\u00A5 tab", PropSeparator.COLON));
    }

    private static final JavaPropertiesResource res = new JavaPropertiesResource();

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
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".properties");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile)) {
            res.write(os, null, WRITE_BUNDLE);
            os.flush();
            // Properties.store() puts a comment with date and time
            // on the first line, ignore it by passing n=1 to compareFiles()
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_WRITE_FILE, tempFile, 1));
        }
    }

    @Test
    public void testMerge() throws IOException {
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".properties");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile); InputStream is = new FileInputStream(INPUT_FILE)) {
            res.merge(is, os, "en", WRITE_BUNDLE);
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_MERGE_FILE, tempFile));
        }
    }

    @Test
    public void testPropDefParseLine() throws IOException {
        assertTrue("The input test file <" + PARSE_TEST_INPUT_FILE + "> does not exist.",
                PARSE_TEST_INPUT_FILE.exists());

        LinkedList<PropDef> actualPropDefs = new LinkedList<PropDef>();

        try (BufferedReader lineRdr = new BufferedReader(new FileReader(PARSE_TEST_INPUT_FILE))) {
            String line = lineRdr.readLine();
            do {
                actualPropDefs.add(PropDef.parseLine(line));
            } while ((line = lineRdr.readLine()) != null);
        }

        assertEquals("PropDefs did not match.", EXPECTED_PROP_DEF_LIST, actualPropDefs);
    }
}