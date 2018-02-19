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
import java.util.Locale;

import org.junit.Test;

import com.ibm.g11n.pipeline.resfilter.FilterOptions;
import com.ibm.g11n.pipeline.resfilter.LanguageBundle;
import com.ibm.g11n.pipeline.resfilter.LanguageBundleBuilder;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterException;
import com.ibm.g11n.pipeline.resfilter.ResourceString;
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
        lst.add(ResourceString.with("Action", "Action").sequenceNumber(1).build());
        lst.add(ResourceString.with("File", "File").sequenceNumber(2).build());
        lst.add(ResourceString.with("longText", LONG_TEXT_STRING).sequenceNumber(3).build());
        lst.add(ResourceString.with("$.Colors.Red", "R").sequenceNumber(4).build());
        lst.add(ResourceString.with("$.Colors.Green", "G").sequenceNumber(5).build());
        lst.add(ResourceString.with("$.Colors.Blue", "B").sequenceNumber(6).build());
        lst.add(ResourceString.with("Done", "Finish").sequenceNumber(7).build());

        Collections.sort(lst, new ResourceStringComparator());
        EXPECTED_INPUT_RES_LIST = lst;
    }

    private static LanguageBundle WRITE_BUNDLE;

    static {
        LanguageBundleBuilder bundleBuilder = new LanguageBundleBuilder(false);
        bundleBuilder.addResourceString("Action", "Aktion", 1);
        bundleBuilder.addResourceString("File", "Datei", 2);
        bundleBuilder.addResourceString("longText", LONG_TEXT_STRING_DE, 3);
        bundleBuilder.addResourceString("$.Colors.Red", "Rot", 4);
        bundleBuilder.addResourceString("$.Colors.Green", "Gr\u00FCn", 5);
        bundleBuilder.addResourceString("$.Colors.Blue", "Blau", 6);
        bundleBuilder.addResourceString("Done", "Fertig", 7);
        bundleBuilder.embeddedLanguageCode("de");
        WRITE_BUNDLE = bundleBuilder.build();
    }

    private static final GlobalizeJsResource res = new GlobalizeJsResource();

    @Test
    public void testParse() throws IOException, ResourceFilterException {
        assertTrue("The input test file <" + INPUT_FILE + "> does not exist.", INPUT_FILE.exists());

        try (InputStream is = new FileInputStream(INPUT_FILE)) {
            LanguageBundle bundle = res.parse(is, null);
            List<ResourceString> resStrList = new ArrayList<>(bundle.getResourceStrings());
            Collections.sort(resStrList, new ResourceStringComparator());
            assertEquals("ResourceStrings did not match.", EXPECTED_INPUT_RES_LIST, resStrList);
        }
    }

    @Test
    public void testWrite() throws IOException, ResourceFilterException {
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".json");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile)) {
            res.write(os, WRITE_BUNDLE, new FilterOptions(Locale.GERMAN));
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_WRITE_FILE, tempFile));
        }
    }

 // TODO: Not ready yet
//    @Test
//    public void testMerge() throws IOException {
//    }
}
