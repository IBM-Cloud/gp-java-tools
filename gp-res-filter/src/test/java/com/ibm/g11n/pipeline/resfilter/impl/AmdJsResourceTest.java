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
 * @author Farhan Arshad
 *
 */
public class AmdJsResourceTest {
    private static final File INPUT_FILE = new File("src/test/resource/resfilter/amdjs/input.js");

    private static final File EXPECTED_WRITE_FILE = new File("src/test/resource/resfilter/amdjs/write-output.js");

    private static final File MERGE_INPUT_1_FILE = new File("src/test/resource/resfilter/amdjs/merge-input-1.js");
    private static final File MERGE_INPUT_2_FILE = new File("src/test/resource/resfilter/amdjs/merge-input-2.js");
    private static final File MERGE_INPUT_3_FILE = new File("src/test/resource/resfilter/amdjs/merge-input-3.js");
    private static final File EXPECTED_MERGE_1_FILE = new File("src/test/resource/resfilter/amdjs/merge-output-1.js");
    private static final File EXPECTED_MERGE_2_FILE = new File("src/test/resource/resfilter/amdjs/merge-output-2.js");
    private static final File EXPECTED_MERGE_3_FILE = new File("src/test/resource/resfilter/amdjs/merge-output-3.js");

    private static final Collection<ResourceString> EXPECTED_INPUT_RES_LIST;

    static {
        List<ResourceString> lst = new LinkedList<ResourceString>();
        lst.add(ResourceString.with("bear 1", "Brown Bear").sequenceNumber(1).build());
        lst.add(ResourceString.with("frog 2", "Red-eyed Tree Frog").sequenceNumber(2).build());
        lst.add(ResourceString.with("owl 3", "Great Horned Owl").sequenceNumber(3).build());

        Collections.sort(lst, new ResourceStringComparator());
        EXPECTED_INPUT_RES_LIST = lst;
    }

    private static LanguageBundle WRITE_BUNDLE;

    static {
        LanguageBundleBuilder bundleBuilder = new LanguageBundleBuilder(false);
        bundleBuilder.addResourceString("owl 3", "Great Horned Owl - translated", 3);
        bundleBuilder.addResourceString("bear 1", "Brown Bear - translated", 1);
        bundleBuilder.addResourceString("frog 2", "Red-eyed Tree Frog - translated", 2);
        WRITE_BUNDLE = bundleBuilder.build();
    }

    private static LanguageBundle MERGE_BUNDLE;

    static {
        LanguageBundleBuilder bundleBuilder = new LanguageBundleBuilder(false);
        String value;
        value = "Great Horned Owl - translated Great Horned Owl - translated "
                + "Great Horned Owl - translated Great Horned Owl - translated "
                + "Great Horned Owl - translated Great Horned Owl - translated "
                + "Great Horned Owl - translated Great Horned Owl - translated "
                + "Great Horned Owl - translated Great Horned Owl - translated ";
        bundleBuilder.addResourceString("owl repeated", value, 3);

        bundleBuilder.addResourceString("owl 3", "Great Horned Owl - translated", 5);

        value = "Translated - IBM Globalization Pipeline provides machine translation and editing "
                + "capabilities that enable you to rapidly translate your web or mobile UI"
                + " and release to your global customers without having to rebuild or re-deploy"
                + " your application. Access Globalization Pipeline capabilities through its "
                + "dashboard, RESTful API, or integrate it seamlessly into your application's "
                + "Delivery Pipeline. File types such as Java properties, JSON, AMD i18n are " + "currently supported.";
        bundleBuilder.addResourceString("bear 1", "Brown Bear - translated", 1);
        bundleBuilder.addResourceString("description", value, 4);
        bundleBuilder.addResourceString("frog 2", "Red-eyed Tree Frog - translated", 2);
        bundleBuilder.addResourceString(
                "Lorem",
                "Loremのイプサムは、単に印刷と植字業界のダミーテキストです。 Loremのイプサムは、未知のプリンターがタイプのゲラを取り、"
                        + "タイプ標本の本を作ってそれをスクランブル1500年代、以来、業界の標準ダミーテキストとなっています。それは本質的に変わらず、"
                        + "何世紀だけでなく、電子組版に飛躍するだけでなく5を生き延びてきました。"
                        + "それはLoremのイプサムのバージョンを含むアルダスのPageMakerのようなデスクトップパブリッシングソフトウェアと、"
                        + "より最近Loremのイプサムの通路を含むLetrasetシートのリリースでは、1960年代に普及したところ。",
                7);
        bundleBuilder.addResourceString("numbers",
                "1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 "
                        + "1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 "
                        + "1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 ",
                6);

        MERGE_BUNDLE = bundleBuilder.build();
    }

    private static final AmdJsResource res = new AmdJsResource();

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
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".js");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile)) {
            res.write(os, WRITE_BUNDLE, null);
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_WRITE_FILE, tempFile));
        }
    }

    @Test
    public void testMerge() throws IOException, ResourceFilterException {
        File tempFile;

        tempFile = File.createTempFile(this.getClass().getSimpleName(), ".js");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile);
                InputStream is = new FileInputStream(MERGE_INPUT_1_FILE)) {
            res.merge(is, os, MERGE_BUNDLE, new FilterOptions(Locale.ENGLISH));
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_MERGE_1_FILE, tempFile));
        }

        tempFile = File.createTempFile(this.getClass().getSimpleName(), ".js");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile);
                InputStream is = new FileInputStream(MERGE_INPUT_2_FILE)) {
            res.merge(is, os, MERGE_BUNDLE, new FilterOptions(Locale.ENGLISH));
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_MERGE_2_FILE, tempFile));
        }

        tempFile = File.createTempFile(this.getClass().getSimpleName(), ".js");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile);
                InputStream is = new FileInputStream(MERGE_INPUT_3_FILE)) {
            res.merge(is, os, MERGE_BUNDLE, new FilterOptions(Locale.JAPANESE));
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_MERGE_3_FILE, tempFile));
        }
    }
}
