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

public class POResourceTest {
    private static final File INPUT_FILE = new File("src/test/resource/resfilter/po/input.po");

    private static final File EXPECTED_WRITE_FILE = new File("src/test/resource/resfilter/po/write-output.po");

    private static Collection<ResourceString> EXPECTED_INPUT_RES_LIST;

    static {
        EXPECTED_INPUT_RES_LIST = new LinkedList<ResourceString>();
        String key, value;

        key = "untranslated-string";
        value = "translated-string";
        EXPECTED_INPUT_RES_LIST.add(new ResourceString(key, value, 1));

        key = "Here is an example of how one might continue a very long string\\n"
                + "for the common case the string represents multi-line output.\\n";
        value = "Translated: Here is an example of how one might continue a very long string\\n"
                + "for the common case the string represents multi-line output.\\n";
        EXPECTED_INPUT_RES_LIST.add(new ResourceString(key, value, 2));

        key = "Enter a comma separated list of user names.";
        value = "Eine kommagetrennte Liste von Benutzernamen.";
        EXPECTED_INPUT_RES_LIST.add(new ResourceString(key, value, 3));

        key = "Unable to find user: @users";
        value = "Benutzer konnte nicht gefunden werden: @users";
        EXPECTED_INPUT_RES_LIST.add(new ResourceString(key, value, 4));

        key = "Unable to find users: @users";
        value = "Benutzer konnten nicht gefunden werden: @users";
        EXPECTED_INPUT_RES_LIST.add(new ResourceString(key, value, 5));
    }

    private static Collection<ResourceString> UNORDERED_RES_LIST;

    static {
        UNORDERED_RES_LIST = new LinkedList<ResourceString>();

        String key = "Untranslated Sea Lion 3";
        String value = "TRANSLATED: California Sea Lion";
        UNORDERED_RES_LIST.add(new ResourceString(key, value, 3));

        key = "Here is an example of how one might continue a very long string "
                + "for the common case the string represents multi-line output 1";
        value = "TRANSLATED: Here is an example of how one might continue a very long string "
                + "for the common case the string represents multi-line output";
        UNORDERED_RES_LIST.add(new ResourceString(key, value, 1));

        key = "Unable to find user: @users 2";
        value = "TRANSLATED: Unable to find users: @users";
        UNORDERED_RES_LIST.add(new ResourceString(key, value, 2));
    }

    private static final POResource res = new POResource();

    @Test
    public void testParse() throws IOException {
        assertTrue("The input test file <" + INPUT_FILE + "> does not exist.", INPUT_FILE.exists());

        try (InputStream is = new FileInputStream(INPUT_FILE)) {
            Collection<ResourceString> resStrs = res.parse(is);
            assertEquals("ResourceStrings did not match.", EXPECTED_INPUT_RES_LIST, resStrs);
        }
    }

    @Test
    public void testWrite() throws IOException {
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".pot");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile)) {
            res.write(os, null, UNORDERED_RES_LIST);
            os.flush();
            // pot files contain a header, this has info which may change,
            // therefore, ignore it by setting n=19 in compareFiles()
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_WRITE_FILE, tempFile, 19));
        }
    }

    @Ignore("not ready yet")
    @Test
    public void testMerge() throws IOException {
    }
}
