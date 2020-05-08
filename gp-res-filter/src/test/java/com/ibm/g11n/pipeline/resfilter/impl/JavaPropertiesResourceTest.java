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
import com.ibm.g11n.pipeline.resfilter.impl.JavaPropertiesResource.Encoding;
import com.ibm.g11n.pipeline.resfilter.impl.JavaPropertiesResource.MessagePatternEscape;
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
    
    private static final File EXPECTED_WRITE_FILE_ALL = new File(
            "src/test/resource/resfilter/properties/write-output-all.properties");

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

        lst.add(ResourceString.with("backslashes", "a\\b\\c")
                .sequenceNumber(12).notes(Arrays.asList(" A comment with backslashes - a\\b\\c あい \\t\\n")).build());

        lst.add(ResourceString.with("extraSpaces", "extra spaces before/after key/value    ")
                .sequenceNumber(13).notes(Arrays.asList(" extra spaces")).build());

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
        bundleBuilder.addResourceString("backslashes", "a\\b\\c", 12,
                Arrays.asList(" A comment with backslashes - a\\b\\c あい \\t\\n"));
        bundleBuilder.addResourceString("extraSpaces", "extra spaces before/after key/value    ", 13,
                Arrays.asList(" extra spaces"));
        bundleBuilder.addNotes(Arrays.asList(
                " You are reading the \".properties\" entry.",
                " The exclamation mark can also mark text as comments.",
                " The key and element characters #, !, =, and : are written with",
                " a preceding backslash to ensure that they are properly loaded."));
        WRITE_BUNDLE = bundleBuilder.build();
    }
    
    private static LanguageBundle WRITE_BUNDLE_ALL;

    static {
        LanguageBundleBuilder bundleBuilder = new LanguageBundleBuilder(false);
        bundleBuilder.addResourceString("A","This is a test.", 1);
        bundleBuilder.addResourceString("B","This isn't a test.", 2);
        bundleBuilder.addResourceString("C","This is't and won't be a test.", 3);
        bundleBuilder.addResourceString("D","This isn't a '{0}'.", 4);
        bundleBuilder.addResourceString("E","This isn't a '{0}' but a {1}.", 5);
        bundleBuilder.addResourceString("F","This '{0}' isn't right.", 6);
        bundleBuilder.addResourceString("G","This '{wasn't}' isn't right.",  7);
        bundleBuilder.addResourceString("H","This '{''}' shouldn't be fine for '{0}'.", 8);
        bundleBuilder.addResourceString("I","{0}", 9);
        bundleBuilder.addResourceString("J","'{0}'", 10);
        bundleBuilder.addResourceString("K","Using '{' and '}'", 11);
        bundleBuilder.addResourceString("L","'{' is ok to use", 12);
        bundleBuilder.addResourceString("M","The name '{0}' is already in use.",13);
        bundleBuilder.addResourceString("N","length must be between '{min} and '{max}",14);
        bundleBuilder.addResourceString("O","Password should not contain:#.-_'().",15);
        bundleBuilder.addResourceString("P","[\\p'{'L'}'\\uFF65]", 16);
        bundleBuilder.addResourceString("Q","value must be '{min}",17);
        bundleBuilder.addResourceString("R","'",18);
        bundleBuilder.addResourceString("S","",19);
        bundleBuilder.addResourceString("T","'test'",20);
        bundleBuilder.addResourceString("U","'value' should not contain '",21);
        bundleBuilder.addResourceString("V","Don't use symbols like +-?.'",22);
        bundleBuilder.addResourceString("W","length shouldn't exceed {max} characters",22);
        bundleBuilder.addResourceString("X","length shouldn't exceed {0} characters",22);
        WRITE_BUNDLE_ALL = bundleBuilder.build();
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
        EXPECTED_PROP_DEF_LIST.add(new PropDef("backslashes", "a\\b\\c", PropSeparator.EQUAL));
        EXPECTED_PROP_DEF_LIST.add(new PropDef("extraSpaces", "extra spaces before/after key/value    ", PropSeparator.EQUAL));
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
    
    private static final String[][] MESSAGE_PATTERN_TEST_CASES = {
        // {<GP expression>,
        //     <props expression - auto>[,
        //     <props expression - all>,                    // if absent, use <prop expression - auto>
        //     <GP expression after round trip - auto>,     // if absent, use <GP expression>
        //     <GP expression after round trip - all>]}     // if absent, use <GP expression>
        { "You're about to delete {0} rows.",
            "You''re about to delete {0} rows." },
        { "You're about to delete '{0}' rows in Mike's file {0}.",
            "You''re about to delete '{0}' rows in Mike''s file {0}." },
        { "Log shows '{''}' in file {0}",
            "Log shows '{''}' in file {0}" },
        { "Log shows '{''} in file {0}",
            "Log shows '{''} in file {0}",
            "Log shows '{''} in file {0}'", // escape with MessagePatternEscape.ALL
            null,
            "Log shows '{''} in file {0}'"}, // round trip with MessagePatternEscape.ALL
        { "Log shows '{'}' in file {0}",
            "Log shows '{'}'' in file {0}" },
        { "Log shows '{'error'}' in file {0}",
            "Log shows '{'error'}' in file {0}" },
        { "Log shows '{''error''}' in file {0}",
            "Log shows '{''error''}' in file {0}" },
        { "File {0} shows '{''error''}'",
            "File {0} shows '{''error''}'" },
        { "The file isn't in use.",
            "The file isn't in use.",
            "The file isn''t in use." }, // escape with MessagePatternEscape.ALL
        { "File {0} isn't in use.",
            "File {0} isn''t in use."},
    };

    @Test
    public void testEscapeMessagePattern() throws ResourceFilterException {
        for (String[] testCase : MESSAGE_PATTERN_TEST_CASES) {
            String result = JavaPropertiesResource.escapeMessagePattern(testCase[0], MessagePatternEscape.AUTO);
            assertEquals("escapeMessagePattern(" + testCase[0] + ", AUTO)", testCase[1], result);
        }

        for (String[] testCase : MESSAGE_PATTERN_TEST_CASES) {
            String result = JavaPropertiesResource.escapeMessagePattern(testCase[0], MessagePatternEscape.ALL);
            String expected = testCase.length >= 3 && testCase[2] != null ? testCase[2] : testCase[1];
            assertEquals("escapeMessagePattern(" + testCase[0] + ", ALL)", expected, result);
        }
    }

    @Test
    public void testUnescapeMessagePattern() throws ResourceFilterException {
        for (String[] testCase : MESSAGE_PATTERN_TEST_CASES) {
            String result = JavaPropertiesResource.unescapeMessagePattern(testCase[1], MessagePatternEscape.AUTO);
            String expected = testCase.length >= 4 && testCase[3] != null ? testCase[3] : testCase[0];
            assertEquals("unescapeMessagePattern(" + testCase[1] + ", AUTO)", expected, result);
        }

        for (String[] testCase : MESSAGE_PATTERN_TEST_CASES) {
            String input = testCase.length >= 3 && testCase[2] != null ? testCase[2] : testCase[1];
            String result = JavaPropertiesResource.unescapeMessagePattern(input, MessagePatternEscape.ALL);
            String expected = testCase.length >= 5 && testCase[4] != null ? testCase[4] : testCase[0];
            assertEquals("unescapeMessagePattern(" + input + ", ALL)", expected, result);
        }
    }

    @Test
    public void testWriteAllQuotes() throws IOException, ResourceFilterException {
        File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".properties");
        JavaPropertiesResource res = new JavaPropertiesResource(Encoding.ISO_8859_1, MessagePatternEscape.ALL);
        tempFile.deleteOnExit();
        try (OutputStream os = new FileOutputStream(tempFile)) {
            res.write(os, WRITE_BUNDLE_ALL, null);
            os.flush();
            // Ignore first line in both the files (first line empty in expected, first line contains timestamp in actual) 
            assertTrue(ResourceTestUtil.compareFiles(EXPECTED_WRITE_FILE_ALL, tempFile, 1));
        }
    }
}