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
public class AndroidStringsResourceTest {
    private static final File INPUT_FILE = new File("src/test/resource/resfilter/xml/input.xml");

    private static final File EXPECTED_WRITE_FILE = new File("src/test/resource/resfilter/xml/write-output.xml");

    private static final File MERGE_INPUT_1_FILE = new File("src/test/resource/resfilter/xml/merge-input-1.xml");
    private static final File MERGE_INPUT_2_FILE = new File("src/test/resource/resfilter/xml/merge-input-2.xml");
    private static final File EXPECTED_MERGE_1_FILE = new File("src/test/resource/resfilter/xml/merge-output-1.xml");
    private static final File EXPECTED_MERGE_2_FILE = new File("src/test/resource/resfilter/xml/merge-output-2.xml");

    private static final Collection<ResourceString> EXPECTED_INPUT_RES_LIST;

    static {
        List<ResourceString> lst = new LinkedList<>();
        lst.add(ResourceString.with("planets_array", "[Mercury, Venus, Earth, Mars]").sequenceNumber(1).build());
        ResourceString rs = ResourceString.with("numberOfSongsAvailable", "{numberOfSongsAvailable, plural, one{Znaleziono %d piosenke.} few{Znaleziono %d piosenki.} other{Znaleziono %d piosenek.}}")
                .sequenceNumber(2)
                .addNote("\n"
                        +"             As a developer, you should always supply \"one\" and \"other\"\n"
                        +"             strings. Your translators will know which strings are actually\n"
                        +"             needed for their language. Always include %d in \"one\" because\n"
                        +"             translators will need to use %d for languages where \"one\"\n"
                        +"             doesn't mean 1 (as explained above).\n"
                        +"          ")
                .build();
        lst.add(rs);
        lst.add(ResourceString.with("bear", "Brown Bear").sequenceNumber(3).build());
        lst.add(ResourceString.with("frog", "Red-eyed Tree Frog").sequenceNumber(4).build());
        lst.add(ResourceString.with("owl", "Great Horned Owl").sequenceNumber(5).build());
        Collections.sort(lst, new ResourceStringComparator());
        EXPECTED_INPUT_RES_LIST = lst;
    }

    private static LanguageBundle WRITE_BUNDLE;

    static {
        LanguageBundleBuilder bundleBuilder = new LanguageBundleBuilder(false);

        bundleBuilder.addResourceString("sealion3", "California Sea Lion", 3);
        bundleBuilder.addResourceString("otter1", "Sea Otter", 1);
        bundleBuilder.addResourceString("crow2", "American Crow", 2);
        bundleBuilder.addResourceString("planets_array4", "[Mercury, Venus, Earth, Mars]", 4);
        ResourceString rs = ResourceString.with("numberOfSongsAvailable", "{numberOfSongsAvailable, plural, one{Znaleziono %d piosenke.} few{Znaleziono %d piosenki.} other{Znaleziono %d piosenek.}}")
                .sequenceNumber(5)
                .addNote("\n"
                        +"             As a developer, you should always supply \"one\" and \"other\"\n"
                        +"             strings. Your translators will know which strings are actually\n"
                        +"             needed for their language. Always include %d in \"one\" because\n"
                        +"             translators will need to use %d for languages where \"one\"\n"
                        +"             doesn't mean 1 (as explained above).\n"
                        +"          ")
                .build();
        bundleBuilder.addResourceString(rs);
        WRITE_BUNDLE = bundleBuilder.build();
    }

    private static LanguageBundle MERGE_BUNDLE;

    static {
        LanguageBundleBuilder bundleBuilder = new LanguageBundleBuilder(false);

        bundleBuilder.addResourceString("planets_array",
                "[Mercury-translated, Venus-translated, Earth-translated, Mars-translated]", 1);
        bundleBuilder.addResourceString("bear", "Brown Bear-translated", 2);
        bundleBuilder.addResourceString("frog", "Red-eyed Tree Frog-translated", 3);
        bundleBuilder.addResourceString("owl", "Great Horned Owl-translated", 4);
        bundleBuilder.addResourceString("Lorem_array",
                "[Loremのイプサムは、単に印刷と植字業界のダミーテキストです。 Loremのイプサムは、未知のプリンターがタイプのゲラを取り,"
                        + "タイプ標本の本を作ってそれをスクランブル1500年代,以来、業界の標準ダミーテキストとなっています。それは本質的に変わらず,"
                        + "何世紀だけでなく,電子組版に飛躍するだけでなく5を生き延びてきました。"
                        + "それはLoremのイプサムのバージョンを含むアルダスのPageMakerのようなデスクトップパブリッシングソフトウェアと、"
                        + "より最近Loremのイプサムの通路を含むLetrasetシートのリリースでは、1960年代に普及したところ。]",
                5);
        bundleBuilder.addResourceString("Lorem",
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
        bundleBuilder.addResourceString("numberOfSongsAvailable",
                "{numberOfSongsAvailable, plural, one{Znaleziono %d piosenke.} few{Znaleziono %d piosenki.} other{Znaleziono %d piosenek.}}",
                8);
        MERGE_BUNDLE = bundleBuilder.build();
    }

    private static final AndroidStringsResource res = new AndroidStringsResource();

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
    public void testWrite() throws IOException, ResourceFilterException{
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".xml");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile)) {
            res.write(os, WRITE_BUNDLE, null);
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_WRITE_FILE, tempFile, 1));
        }
    }

    @Test
    public void testMerge() throws IOException, ResourceFilterException {
        File tempFile;

        tempFile = File.createTempFile(this.getClass().getSimpleName(), ".xml");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile);
                InputStream is = new FileInputStream(MERGE_INPUT_1_FILE)) {
            res.merge(is, os, MERGE_BUNDLE, new FilterOptions(Locale.ENGLISH));
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_MERGE_1_FILE, tempFile));
        }

        tempFile = File.createTempFile(this.getClass().getSimpleName(), ".xml");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile);
                InputStream is = new FileInputStream(MERGE_INPUT_2_FILE)) {
            res.merge(is, os, MERGE_BUNDLE, new FilterOptions(Locale.JAPANESE));
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_MERGE_2_FILE, tempFile));
        }
    }
}