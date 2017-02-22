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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.ibm.g11n.pipeline.resfilter.JavaPropertiesResource.PropDef;
import com.ibm.g11n.pipeline.resfilter.JavaPropertiesResource.PropDef.PropSeparator;
import com.ibm.g11n.pipeline.resfilter.ResourceString.ResourceStringComparator;

/**
 * @author Farhan Arshad, JCEmmons
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
        lst.add(new ResourceString("message", "Welcome to Wikipedia!", 3,
                Arrays.asList(
                        " The backslash below tells the application to continue reading",
                        " the value onto the next line.")));
        lst.add(new ResourceString("key with spaces",
                "This is the value that could be looked up with the key \"key with spaces\".", 4,
                Arrays.asList(" Add spaces to the key")));

        ResourceString rs5 = new ResourceString("tab", "pick up the\u00A5 tab", 5);
        rs5.addNote(" Unicode");
        lst.add(rs5);

        lst.add(new ResourceString("leadSPs", "leading SPs", 6,
                Arrays.asList(" leading SPs")));

        lst.add(new ResourceString("leadTabs", "leading tabs", 7,
                Arrays.asList(" leading tabs")));

        lst.add(new ResourceString("trailSPs", "trailing SPs  ", 8,
                Arrays.asList(" trailing SPs")));

        lst.add(new ResourceString("withTabs", "Tab1\tTab2\tTab3\t", 9,
                Arrays.asList(" tabs")));

        Collections.sort(lst, new ResourceStringComparator());
        EXPECTED_INPUT_RES_LIST = lst;
    }

    private static Bundle WRITE_BUNDLE;

    static {
        WRITE_BUNDLE = new Bundle();
        WRITE_BUNDLE.addResourceString("language", "Not-English", 2);
        WRITE_BUNDLE.addResourceString("key with spaces",
                "Translated - This is the value that could be looked up with the key \"key with spaces\".", 4,
                Arrays.asList(" Add spaces to the key"));
        WRITE_BUNDLE.addResourceString("website", "http://en.wikipedia.org/translated", 1);
        WRITE_BUNDLE.addResourceString("message", "Translated - Welcome to Wikipedia!", 3,
                Arrays.asList(" The backslash below tells the application to continue reading",
                        " the value onto the next line."));
        WRITE_BUNDLE.addResourceString("tab", "Translated - pick up the\u00A5 tab", 5,
                Arrays.asList(" Unicode"));
        WRITE_BUNDLE.addResourceString("leadSPs", "leading SPs", 6,
                Arrays.asList(" leading SPs"));
        WRITE_BUNDLE.addResourceString("leadTabs", "localized leading tabs", 7,
                Arrays.asList(" leading tabs"));
        WRITE_BUNDLE.addResourceString("trailSPs", "localized trailing SPs  ", 8,
                Arrays.asList(" trailing SPs"));
        WRITE_BUNDLE.addResourceString("withTabs", "localized Tab1\tTab2\tTab3\t", 9,
                Arrays.asList(" tabs"));
        WRITE_BUNDLE.addNotes(Arrays.asList(
                " You are reading the \".properties\" entry.",
                " The exclamation mark can also mark text as comments.",
                " The key and element characters #, !, =, and : are written with",
                " a preceding backslash to ensure that they are properly loaded."));

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
        EXPECTED_PROP_DEF_LIST.add(new PropDef("leadSPs", "leading SPs", PropSeparator.EQUAL));
        EXPECTED_PROP_DEF_LIST.add(new PropDef("leadTabs", "leading tabs", PropSeparator.EQUAL));
        EXPECTED_PROP_DEF_LIST.add(new PropDef("trailSPs", "trailing SPs  ", PropSeparator.EQUAL));
        EXPECTED_PROP_DEF_LIST.add(new PropDef("withTabs", "Tab1\tTab2\tTab3\t", PropSeparator.EQUAL));
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
            assertTrue(ResourceTestUtil.compareFilesUpTo(EXPECTED_WRITE_FILE, tempFile, 5));
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_WRITE_FILE, tempFile, 6));
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

    @Test
    public void testEscapePropsKey() {
        final String[][] testCases = {
            {"", ""},
            {"abc", "abc"},
            {"a b c", "a\\ b\\ c"},
            {" a b ", "\\ a\\ b\\ "},
            {" \t abc \t ", "\\ \\t\\ abc\\ \\t\\ "},
            {"\u0000\u0001", "\\u0000\\u0001"},
            {"a=b=c", "a\\=b\\=c"},
            {"a:b;c", "a\\:b;c"},
            {"!#$%()*+,-./", "\\!\\#$%()*+,-./"},
            {"' abc '", "'\\ abc\\ '"},
            {"a \"bc\"", "a\\ \"bc\""},
            {"\u3042\u3044", "\\u3042\\u3044"},
        };

        for (String[] testCase : testCases) {
            String instr = testCase[0];
            String expected = testCase[1];

            String escapedKey = JavaPropertiesResource.escapePropKey(instr);
            assertEquals("escapePropKey(" + instr + ")", expected, escapedKey);

            String unescapedKey = JavaPropertiesResource.unescapePropKey(escapedKey);
            assertEquals("unescapePropKey(" + escapedKey + ")", instr, unescapedKey);
        }
    }

    @Test
    public void testEscapePropsValue() {
        final String[][] testCases = {
                {"", ""},
                {"abc", "abc"},
                {"a b c", "a b c"},
                {" a b ", "\\ a b "},
                {" \t abc \t ", "\\ \\t\\ abc \\t "},
                {"\u0000\u0001", "\\u0000\\u0001"},
                {"a=b=c", "a\\=b\\=c"},
                {"a:b;c", "a\\:b;c"},
                {"!#$%()*+,-./", "\\!\\#$%()*+,-./"},
                {"' abc '", "' abc '"},
                {"a \"bc\"", "a \"bc\""},
                {"\u3042\u3044", "\\u3042\\u3044"},
            };

            for (String[] testCase : testCases) {
                String instr = testCase[0];
                String expected = testCase[1];

                String escapedVal = JavaPropertiesResource.escapePropValue(instr);
                assertEquals("escapePropValue(" + instr + ")", expected, escapedVal);

                String unescapedVal = JavaPropertiesResource.unescapePropValue(escapedVal);
                assertEquals("unescapePropValue(" + escapedVal + ")", instr, unescapedVal);
            }
    }

    private static final String[][] UNESC_TEST_CASES =
    {
        {"", ""},
        {"abc", "abc"},
        {"\\ abc\\u0020", " abc "},
        {"a\\tb\\u0009c", "a\tb\tc"},
        {"a\\=\\b", "a=b"},
        {"a\\\\b", "a\\b"},
        {"\\t\\f\\z", "\t\fz"},
        {"\\a\\b\\c", "abc"},
        {"\\u304A\\u304b", "\u304A\u304B"},
    };

    @Test
    public void testUnescapePropsKey() {
        for (String[] testCase : UNESC_TEST_CASES) {
            String instr = testCase[0];
            String expected = testCase[1];

            String unescapedKey = JavaPropertiesResource.unescapePropKey(instr);
            assertEquals("unescapePropKey(" + instr + ")", expected, unescapedKey);
        }
    }

    @Test
    public void testUnescapePropsValue() {
        for (String[] testCase : UNESC_TEST_CASES) {
            String instr = testCase[0];
            String expected = testCase[1];

            String unescapedVal = JavaPropertiesResource.unescapePropValue(instr);
            assertEquals("unescapePropValue(" + instr + ")", expected, unescapedVal);
        }
    }
}