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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.ibm.g11n.pipeline.resfilter.ResourceString.ResourceStringComparator;

/**
 * @author Farhan Arshad
 *
 */
public class POTResourceTest {
    private static final File INPUT_FILE = new File("src/test/resource/resfilter/pot/input.pot");

    private static final File EXPECTED_WRITE_FILE = new File("src/test/resource/resfilter/pot/write-output.pot");

    private static final File MERGE_INPUT_1_FILE = new File("src/test/resource/resfilter/pot/merge-input-1.pot");
    private static final File MERGE_INPUT_2_FILE = new File("src/test/resource/resfilter/pot/merge-input-2.pot");
    private static final File EXPECTED_MERGE_1_FILE = new File("src/test/resource/resfilter/pot/merge-output-1.pot");
    private static final File EXPECTED_MERGE_2_FILE = new File("src/test/resource/resfilter/pot/merge-output-2.pot");

    private static final Collection<ResourceString> EXPECTED_INPUT_RES_LIST;

    static {
        List<ResourceString> lst = new LinkedList<>();

        String key;

        key = "untranslated-string";
        lst.add(new ResourceString(key, key, 1));

        key = "Here is an example of how one might continue a very long string\\n"
                + "for the common case the string represents multi-line output.\\n";
        lst.add(new ResourceString(key, key, 2));

        key = "Enter a comma separated list of user names.";
        lst.add(new ResourceString(key, key, 3));

        key = "Unable to find user: @users";
        lst.add(new ResourceString(key, key, 4));

        key = "Unable to find users: @users";
        lst.add(new ResourceString(key, key, 5));

        Collections.sort(lst, new ResourceStringComparator());
        EXPECTED_INPUT_RES_LIST = lst;
    }

    private static Bundle WRITE_BUNDLE;

    static {
        WRITE_BUNDLE = new Bundle();

        String key = "Untranslated Sea Lion 3";
        String value = "TRANSLATED: California Sea Lion";
        WRITE_BUNDLE.addResourceString(key, value, 3);

        key = "Here is an example of how one might continue a very long string "
                + "for the common case the string represents multi-line output 1";
        value = "TRANSLATED: Here is an example of how one might continue a very long string "
                + "for the common case the string represents multi-line output";
        WRITE_BUNDLE.addResourceString(key, value, 1);

        key = "Unable to find user: @users 2";
        value = "TRANSLATED: Unable to find users: @users";
        WRITE_BUNDLE.addResourceString(key, value, 2);
    }

    private static Bundle MERGE_BUNDLE;

    static {
        MERGE_BUNDLE = new Bundle();
        String key, value;

        key = "Here is an example of how one might continue a very long string\\n"
                + "for the common case the string represents multi-line output.\\n";
        value = "Voici un exemple de la façon dont on pourrait continuer à une très longue chaîne\\n"
                + "Pour le cas courant de la chaîne représente la sortie multi-ligne.\\n";
        MERGE_BUNDLE.addResourceString(key, value, 1);

        key = "Enter a comma separated list of user names.";
        value = "Entrez une virgule liste séparée par des noms d'utilisateur.";
        MERGE_BUNDLE.addResourceString(key, value, 2);

        key = "Unable to find user: @users";
        value = "Impossible de trouver l'utilisateur : @users";
        MERGE_BUNDLE.addResourceString(key, value, 3);

        key = "Unable to find users: @users";
        value = "Impossible de trouver les utilisateurs: @users";
        MERGE_BUNDLE.addResourceString(key, value, 4);

        key = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. "
                + "Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, "
                + "when an unknown printer took a galley of type and scrambled it to make a type specimen book. "
                + "It has survived not only five centuries, but also the leap into electronic typesetting, "
                + "remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset "
                + "sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like"
                + " Aldus PageMaker including versions of Lorem Ipsum.";
        value = "Loremのイプサムは、単に印刷と植字業界のダミーテキストです。 Loremのイプサムは、未知のプリンターがタイプのゲラを取り、"
                + "タイプ標本の本を作ってそれをスクランブル1500年代、以来、業界の標準ダミーテキストとなっています。それは本質的に変わらず、"
                + "何世紀だけでなく、電子組版に飛躍するだけでなく5を生き延びてきました。"
                + "それはLoremのイプサムのバージョンを含むアルダスのPageMakerのようなデスクトップパブリッシングソフトウェアと、"
                + "より最近Loremのイプサムの通路を含むLetrasetシートのリリースでは、1960年代に普及したところ。";
        MERGE_BUNDLE.addResourceString(key, value, 6);

        key = "numbers";
        value = "1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 "
                + "1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 "
                + "1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 ";
        MERGE_BUNDLE.addResourceString(key, value, 5);
    }

    private static final POTResource res = new POTResource();

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
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".pot");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile)) {
            res.write(os, null, WRITE_BUNDLE);
            os.flush();
            // pot files contain a header, this has info which may change,
            // therefore, ignore it by setting n=19 in compareFiles()
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_WRITE_FILE, tempFile, 19));
        }
    }

    @Test
    public void testMerge() throws IOException {
        File tempFile;

        tempFile = File.createTempFile(this.getClass().getSimpleName(), ".pot");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile);
                InputStream is = new FileInputStream(MERGE_INPUT_1_FILE)) {
            res.merge(is, os, "en", MERGE_BUNDLE);
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_MERGE_1_FILE, tempFile));
        }

        tempFile = File.createTempFile(this.getClass().getSimpleName(), ".pot");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile);
                InputStream is = new FileInputStream(MERGE_INPUT_2_FILE)) {
            res.merge(is, os, "ja", MERGE_BUNDLE);
            os.flush();
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_MERGE_2_FILE, tempFile));
        }
    }

}