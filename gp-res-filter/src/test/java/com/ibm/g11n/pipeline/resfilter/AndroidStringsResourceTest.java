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
public class AndroidStringsResourceTest {
    private static final File INPUT_FILE = new File("src/test/resource/resfilter/xml/input.xml");

    private static final File EXPECTED_WRITE_FILE = new File("src/test/resource/resfilter/xml/write-output.xml");

    private static final File MERGE_INPUT_1_FILE = new File("src/test/resource/resfilter/xml/merge-input-1.xml");
    private static final File MERGE_INPUT_2_FILE = new File("src/test/resource/resfilter/xml/merge-input-2.xml");
    private static final File EXPECTED_MERGE_1_FILE = new File("src/test/resource/resfilter/xml/merge-output-1.xml");
    private static final File EXPECTED_MERGE_2_FILE = new File("src/test/resource/resfilter/xml/merge-output-2.xml");

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

    private static Collection<ResourceString> MERGE_RES_LIST;

    static {
        MERGE_RES_LIST = new LinkedList<ResourceString>();
        MERGE_RES_LIST.add(new ResourceString("planets_array",
                "[Mercury-translated, Venus-translated, Earth-translated, Mars-translated]", 1));
        MERGE_RES_LIST.add(new ResourceString("bear", "Brown Bear-translated", 2));
        MERGE_RES_LIST.add(new ResourceString("frog", "Red-eyed Tree Frog-translated", 3));
        MERGE_RES_LIST.add(new ResourceString("owl", "Great Horned Owl-translated", 4));
        MERGE_RES_LIST.add(new ResourceString("Lorem_array",
                "[Loremのイプサムは、単に印刷と植字業界のダミーテキストです。 Loremのイプサムは、未知のプリンターがタイプのゲラを取り,"
                        + "タイプ標本の本を作ってそれをスクランブル1500年代,以来、業界の標準ダミーテキストとなっています。それは本質的に変わらず,"
                        + "何世紀だけでなく,電子組版に飛躍するだけでなく5を生き延びてきました。"
                        + "それはLoremのイプサムのバージョンを含むアルダスのPageMakerのようなデスクトップパブリッシングソフトウェアと、"
                        + "より最近Loremのイプサムの通路を含むLetrasetシートのリリースでは、1960年代に普及したところ。]",
                5));
        MERGE_RES_LIST.add(new ResourceString("Lorem",
                "Loremのイプサムは、単に印刷と植字業界のダミーテキストです。 Loremのイプサムは、未知のプリンターがタイプのゲラを取り、"
                        + "タイプ標本の本を作ってそれをスクランブル1500年代、以来、業界の標準ダミーテキストとなっています。それは本質的に変わらず、"
                        + "何世紀だけでなく、電子組版に飛躍するだけでなく5を生き延びてきました。"
                        + "それはLoremのイプサムのバージョンを含むアルダスのPageMakerのようなデスクトップパブリッシングソフトウェアと、"
                        + "より最近Loremのイプサムの通路を含むLetrasetシートのリリースでは、1960年代に普及したところ。",
                7));
        MERGE_RES_LIST.add(new ResourceString("numbers",
                "1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 "
                        + "1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 "
                        + "1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 ",
                6));
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
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_WRITE_FILE, tempFile, 1));
        }
    }

    @Test
    public void testMerge() throws IOException {
        File tempFile;

        tempFile = File.createTempFile(this.getClass().getSimpleName(), ".xml");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile);
                InputStream is = new FileInputStream(MERGE_INPUT_1_FILE)) {
            res.merge(is, os, "en", MERGE_RES_LIST);
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_MERGE_1_FILE, tempFile));
        }

        tempFile = File.createTempFile(this.getClass().getSimpleName(), ".xml");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile);
                InputStream is = new FileInputStream(MERGE_INPUT_2_FILE)) {
            res.merge(is, os, "ja", MERGE_RES_LIST);
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_MERGE_2_FILE, tempFile));
        }
    }
}