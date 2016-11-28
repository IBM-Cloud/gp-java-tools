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
 * @author Farhan Arshad
 *
 */
public class IOSStringsResourceTest {
    private static final File INPUT = new File("src/test/resource/resfilter/ios/input.strings");

    private static final File EXPECTED_WRITE_FILE = new File("src/test/resource/resfilter/ios/write-output.strings");

    private static final File MERGE_INPUT_1_FILE = new File("src/test/resource/resfilter/ios/merge-input-1.strings");
    private static final File MERGE_INPUT_2_FILE = new File("src/test/resource/resfilter/ios/merge-input-2.strings");
    private static final File EXPECTED_MERGE_1_FILE = new File("src/test/resource/resfilter/ios/merge-output-1.strings");
    private static final File EXPECTED_MERGE_2_FILE = new File("src/test/resource/resfilter/ios/merge-output-2.strings");

    private static Collection<ResourceString> EXPECTED_INPUT_RES_LIST;

    static {
        EXPECTED_INPUT_RES_LIST = new LinkedList<ResourceString>();
        EXPECTED_INPUT_RES_LIST.add(new ResourceString("Insert Element", "Insert Element", 1));
        EXPECTED_INPUT_RES_LIST.add(new ResourceString("ErrorString_1", "An unknown error occurred.", 2));
        EXPECTED_INPUT_RES_LIST.add(new ResourceString("bear 3", "Brown Bear", 3));
        EXPECTED_INPUT_RES_LIST.add(new ResourceString("frog 4", "Red-eyed Tree Frog", 4));
        EXPECTED_INPUT_RES_LIST.add(new ResourceString("owl 5", "Great Horned Owl", 5));
    }

    private static Bundle WRITE_BUNDLE;

    static {
        WRITE_BUNDLE = new Bundle();
        WRITE_BUNDLE.addResourceString("sealion 3", "California Sea Lion", 3);
        WRITE_BUNDLE.addResourceString("otter 1", "Sea Otter", 1);
        WRITE_BUNDLE.addResourceString("crow 2", "American Crow", 2);
        WRITE_BUNDLE.addResourceString("Lorem",
                "Loremのイプサムは、単に印刷と植字業界のダミーテキストです。 Loremのイプサムは、未知のプリンターがタイプのゲラを取り、"
                        + "タイプ標本の本を作ってそれをスクランブル1500年代、以来、業界の標準ダミーテキストとなっています。それは本質的に変わらず、"
                        + "何世紀だけでなく、電子組版に飛躍するだけでなく5を生き延びてきました。"
                        + "それはLoremのイプサムのバージョンを含むアルダスのPageMakerのようなデスクトップパブリッシングソフトウェアと、"
                        + "より最近Loremのイプサムの通路を含むLetrasetシートのリリースでは、1960年代に普及したところ。",
                5);
        WRITE_BUNDLE.addResourceString("numbers",
                "1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 "
                        + "1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 "
                        + "1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 ",
                4);
    }

    private static Bundle MERGE_BUNDLE;

    static {
        MERGE_BUNDLE = new Bundle();
        MERGE_BUNDLE.addResourceString("Insert Element", "Insert Element - translated", 1);
        MERGE_BUNDLE.addResourceString("ErrorString_1", "An unknown error occurred - translated.", 2);
        MERGE_BUNDLE.addResourceString("bear 3", "Brown Bear - translated", 3);
        MERGE_BUNDLE.addResourceString("frog 4", "Red-eyed Tree Frog - translated", 4);
        MERGE_BUNDLE.addResourceString("owl 5", "Great Horned Owl - translated", 5);
        MERGE_BUNDLE.addResourceString(
                "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the "
                        + "industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of "
                        + "type and scrambled it to make a type specimen book. It has survived not only five centuries, "
                        + "but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised"
                        + " in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently"
                        + " with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.",
                "Loremのイプサムは、単に印刷と植字業界のダミーテキストです。 Loremのイプサムは、未知のプリンターがタイプのゲラを取り、"
                        + "タイプ標本の本を作ってそれをスクランブル1500年代、以来、業界の標準ダミーテキストとなっています。それは本質的に変わらず、"
                        + "何世紀だけでなく、電子組版に飛躍するだけでなく5を生き延びてきました。"
                        + "それはLoremのイプサムのバージョンを含むアルダスのPageMakerのようなデスクトップパブリッシングソフトウェアと、"
                        + "より最近Loremのイプサムの通路を含むLetrasetシートのリリースでは、1960年代に普及したところ。",
                7);
        MERGE_BUNDLE.addResourceString("numbers",
                "1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 "
                        + "1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 "
                        + "1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 ",
                6);
    }

    private static final IOSStringsResource res = new IOSStringsResource();

    @Test
    public void testParse() throws IOException {
        assertTrue("The input test file <" + INPUT + "> does not exist.", INPUT.exists());

        try (InputStream is = new FileInputStream(INPUT)) {
            Bundle bundle = res.parse(is);
            assertEquals("ResourceStrings did not match.", EXPECTED_INPUT_RES_LIST, bundle.getResourceStrings());
        }
    }

    @Test
    public void testWrite() throws IOException {
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".strings");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile)) {
            res.write(os, null, WRITE_BUNDLE);
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_WRITE_FILE, tempFile));
        }
    }

    @Test
    public void testMerge() throws IOException {
        File tempFile;

        tempFile = File.createTempFile(this.getClass().getSimpleName(), ".strings");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile);
                InputStream is = new FileInputStream(MERGE_INPUT_1_FILE)) {
            res.merge(is, os, "en", MERGE_BUNDLE);
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_MERGE_1_FILE, tempFile));
        }

        tempFile = File.createTempFile(this.getClass().getSimpleName(), ".strings");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile);
                InputStream is = new FileInputStream(MERGE_INPUT_2_FILE)) {
            res.merge(is, os, "ja", MERGE_BUNDLE);
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_MERGE_2_FILE, tempFile));
        }
    }
}
