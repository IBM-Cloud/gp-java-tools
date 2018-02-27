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
package com.ibm.g11n.pipeline.resfilter.impl;

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
import java.util.Locale;

import org.junit.Test;

import com.ibm.g11n.pipeline.resfilter.FilterOptions;
import com.ibm.g11n.pipeline.resfilter.LanguageBundle;
import com.ibm.g11n.pipeline.resfilter.LanguageBundleBuilder;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterException;
import com.ibm.g11n.pipeline.resfilter.ResourceString;
import com.ibm.g11n.pipeline.resfilter.ResourceString.ResourceStringComparator;
import com.ibm.g11n.pipeline.resfilter.impl.JavaPropertiesResource.PropDef;
import com.ibm.g11n.pipeline.resfilter.impl.JavaPropertiesResource.PropDef.PropSeparator;

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
        lst.add(ResourceString.with("website", "http://en.wikipedia.org/").sequenceNumber(1).build());
        lst.add(ResourceString.with("language", "English").sequenceNumber(2).build());
        lst.add(ResourceString.with("message", "Welcome to Wikipedia!").sequenceNumber(3)
                .notes(Arrays.asList(
                        " The backslash below tells the application to continue reading",
                        " the value onto the next line.")).build());
        lst.add(ResourceString.with("key with spaces",
                "This is the value that could be looked up with the key \"key with spaces\".")
                .sequenceNumber(4)
                .notes(Arrays.asList(" Add spaces to the key")).build());

        ResourceString rs5 = ResourceString.with("tab", "pick up the\u00A5 tab").sequenceNumber(5)
                .addNote(" Unicode").build();
        lst.add(rs5);

        lst.add(ResourceString.with("leadSPs", "leading SPs").sequenceNumber(6)
                .notes(Arrays.asList(" leading SPs")).build());

        lst.add(ResourceString.with("leadTabs", "leading tabs").sequenceNumber(7)
                .notes(Arrays.asList(" leading tabs")).build());

        lst.add(ResourceString.with("trailSPs", "trailing SPs  ").sequenceNumber(8)
                .notes(Arrays.asList(" trailing SPs")).build());

        lst.add(ResourceString.with("withTabs", "Tab1\tTab2\tTab3\t").sequenceNumber(9)
                .notes(Arrays.asList(" tabs")).build());

        lst.add(ResourceString.with("withQuote", "You're about to delete '{0}' rows in Mike's file {0}.")
                .sequenceNumber(10).notes(Arrays.asList(" Quote")).build());

        lst.add(ResourceString.with("non-param", "This {} is not a parameter.")
                .sequenceNumber(11).notes(Arrays.asList(" Not a Java MessageFormat param")).build());

        Collections.sort(lst, new ResourceStringComparator());
        EXPECTED_INPUT_RES_LIST = lst;
    }

    private static LanguageBundle WRITE_BUNDLE;

    static {
        LanguageBundleBuilder bundleBuilder = new LanguageBundleBuilder(false);
        bundleBuilder.addResourceString("language", "Not-English", 2);
        bundleBuilder.addResourceString("key with spaces",
                "Translated - This is the value that could be looked up with the key \"key with spaces\".", 4,
                Arrays.asList(" Add spaces to the key"));
        bundleBuilder.addResourceString("website", "http://en.wikipedia.org/translated", 1);
        bundleBuilder.addResourceString("message", "Translated - Welcome to Wikipedia!", 3,
                Arrays.asList(" The backslash below tells the application to continue reading",
                        " the value onto the next line."));
        bundleBuilder.addResourceString("tab", "Translated - pick up the\u00A5 tab", 5,
                Arrays.asList(" Unicode"));
        bundleBuilder.addResourceString("leadSPs", "localized leading SPs", 6,
                Arrays.asList(" leading SPs"));
        bundleBuilder.addResourceString("leadTabs", "localized leading tabs", 7,
                Arrays.asList(" leading tabs"));
        bundleBuilder.addResourceString("trailSPs", "localized trailing SPs  ", 8,
                Arrays.asList(" trailing SPs"));
        bundleBuilder.addResourceString("withTabs", "localized Tab1\tTab2\tTab3\t", 9,
                Arrays.asList(" tabs"));
        bundleBuilder.addResourceString("withQuote", "You're about to delete '{1}' rows in Mike's file {0}.", 10,
                Arrays.asList(" Quote"));
        bundleBuilder.addResourceString("non-param", "This {} is not a parameter.", 11,
                Arrays.asList(" Not a Java MessageFormat param"));
        bundleBuilder.addNotes(Arrays.asList(
                " You are reading the \".properties\" entry.",
                " The exclamation mark can also mark text as comments.",
                " The key and element characters #, !, =, and : are written with",
                " a preceding backslash to ensure that they are properly loaded."));
        WRITE_BUNDLE = bundleBuilder.build();
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
        // PropDef does not detect message pattern - message pattern handling is done by the logic in JavaPropertiesResource class
        EXPECTED_PROP_DEF_LIST.add(new PropDef("withQuote", "You''re about to delete '{1}' rows in Mike''s file {0}.", PropSeparator.EQUAL));
        EXPECTED_PROP_DEF_LIST.add(new PropDef("non-param", "This {} is not a parameter.", PropSeparator.EQUAL));
    }

    private static final JavaPropertiesResource res = new JavaPropertiesResource();

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
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".properties");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile)) {
            res.write(os, WRITE_BUNDLE, null);
            os.flush();
            // Properties.store() puts a comment with date and time
            // on the first line, ignore it by passing n=1 to compareFiles()
            assertTrue(ResourceTestUtil.compareFilesUpTo(EXPECTED_WRITE_FILE, tempFile, 5));
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_WRITE_FILE, tempFile, 6));
        }
    }

    @Test
    public void testMerge() throws IOException, ResourceFilterException {
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".properties");
        tempFile.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tempFile); InputStream is = new FileInputStream(INPUT_FILE)) {
            res.merge(is, os, WRITE_BUNDLE, new FilterOptions(Locale.ENGLISH));
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
    
    private static final String[][] QUOTES_TEST_CASES =
        {
                {"It's not a bug.","It's not a bug."},
                {"You're about to delete {0} rows.","You''re about to delete {0} rows."},
                {"You're about to delete '{0}' rows in Mike's file {0}.","You''re about to delete '{0}' rows in Mike''s file {0}."},
                {"Log shows '{''}' in file {0}","Log shows '{''}' in file {0}"},
                {"Log shows '{''} in file {0}","Log shows '{''} in file {0}"},
                {"Log shows '{'}' in file {0}","Log shows '{'}'' in file {0}"},
                {"Log shows '{'error'}' in file {0}","Log shows '{'error'}' in file {0}"},
                {"Log shows '{''error''}' in file {0}","Log shows '{''error''}' in file {0}"},
                {"File {0} shows '{''error''}'","File {0} shows '{''error''}'"}
        };
     
    @Test    
    public void testConvertDoubleSingleQuote(){
        for (String[] testCase : QUOTES_TEST_CASES) {
            String instr = testCase[1];
            String expected = testCase[0];

            String doubleQuoteVal = JavaPropertiesResource.ConvertDoubleSingleQuote(instr);
            assertEquals("ConvertDoubleSingleQuote(" + instr + ")", expected, doubleQuoteVal);
        }
    }
    
    @Test
    public void testConvertSingleQuote(){
        for (String[] testCase : QUOTES_TEST_CASES) {
            String instr = testCase[0];
            String expected = testCase[1];

            String doubleQuoteVal = JavaPropertiesResource.ConvertSingleQuote(instr);
            assertEquals("ConvertSingleQuote(" + instr + ")", expected, doubleQuoteVal);
        }
    }
    
}