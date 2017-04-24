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
 * @author farhan, jcemmons
 *
 */
public class XLIFFResourceTest {
    private static final File INPUT_FILE = new File("src/test/resource/resfilter/xliff/input.xlf");

    private static final File EXPECTED_WRITE_FILE = new File("src/test/resource/resfilter/xliff/write-output.xlf");

    private static final File MERGE_INPUT_1_FILE = new File("src/test/resource/resfilter/xliff/merge-input-1.xlf");
    private static final File MERGE_INPUT_2_FILE = new File("src/test/resource/resfilter/xliff/merge-input-2.xlf");
    @SuppressWarnings("unused")
    private static final File EXPECTED_MERGE_1_FILE = new File("src/test/resource/resfilter/xliff/merge-output-1.xlf");
    @SuppressWarnings("unused")
    private static final File EXPECTED_MERGE_2_FILE = new File("src/test/resource/resfilter/xliff/merge-output-2.xlf");

    private static final Collection<ResourceString> EXPECTED_INPUT_RES_LIST;

    static {
        List<ResourceString> lst = new LinkedList<>();

        lst.add(new ResourceString("1", "Quetzal", 1));
        lst.add(new ResourceString("3", "An application to manipulate and process XLIFF documents", 2));
        lst.add(new ResourceString("4", "XLIFF Data Manager", 3));
        Collections.sort(lst, new ResourceStringComparator());
        EXPECTED_INPUT_RES_LIST = lst;

    }

    private static Bundle WRITE_BUNDLE;

    static {
        WRITE_BUNDLE = new Bundle();

        WRITE_BUNDLE.addResourceString("3", "XLIFF 文書を編集、または処理 するアプリケーションです。", 2, null, "An application to manipulate and process XLIFF documents");
        WRITE_BUNDLE.addResourceString("4", "XLIFF データ・マネージャ", 3, null, "XLIFF Data Manager");
        WRITE_BUNDLE.addResourceString("1", "Quetzal", 1, null, "Quetzal");
    }

    private static Bundle MERGE_BUNDLE;

    static {
        MERGE_BUNDLE = new Bundle();
        MERGE_BUNDLE.addResourceString("1", "Quetzal", 1);
        MERGE_BUNDLE.addResourceString("2", "XLIFF 文書を編集、または処理 するアプリケーションです。", 2);
        MERGE_BUNDLE.addResourceString("3", "XLIFF データ・マネージャ", 3);
    }

    private static final XLIFFResource res = new XLIFFResource();

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
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".xlf");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile)) {
            res.write(os, "ja", WRITE_BUNDLE);
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_WRITE_FILE, tempFile));
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testMerge() throws IOException {
        File tempFile;

        tempFile = File.createTempFile(this.getClass().getSimpleName(), ".xlf");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile);
                InputStream is = new FileInputStream(MERGE_INPUT_1_FILE)) {
            res.merge(is, os, "en", MERGE_BUNDLE);
            os.flush();
            // TODO: Not ready yet
            // assertTrue(ResourceTestUtil.compareFiles(EXPECTED_MERGE_1_FILE, tempFile));
        }

        tempFile = File.createTempFile(this.getClass().getSimpleName(), ".xlf");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile);
                InputStream is = new FileInputStream(MERGE_INPUT_2_FILE)) {
            res.merge(is, os, "en", MERGE_BUNDLE);
            os.flush();
            // TODO: Not ready yet
            // assertTrue(ResourceTestUtil.compareFiles(EXPECTED_MERGE_2_FILE, tempFile));
        }
    }
}
