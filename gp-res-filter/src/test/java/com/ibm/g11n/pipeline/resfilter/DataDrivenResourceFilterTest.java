/*  
 * Copyright IBM Corp. 2019
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;

/**
 * Data driven resource filter test
 * 
 * @author Yoshito Umaoka
 */
public class DataDrivenResourceFilterTest {
    //
    // Data drive test runs resource filter methods on a set of test data files. A test data set may contain
    // following files.
    //
    // 1) bundle.json (required)
    //
    // Serialized LanguageBundle object in JSON format. This content is used as expected result for
    // parse test, and input for write/merge test. If this file is not found, the test case will be
    // skipped.
    //
    // 2) input.<ext>
    //
    // Resource file used for parse test. <ext> is appropriate file extension for the resource type,
    // such as .properties for Java property resource bundle, .json for JSON resource bundle.
    // This file is used as input for ResoruceFilter#parse method. The result will be compared with
    // the content of bundle.json. If absent, parse test will be skipped.
    //
    // 3) parse_options.json
    //
    // JSON expression of FilterOptions object used for parse test.
    //
    // 4) expected_write.<ext>
    //
    // Expected output for write test. A write test load the contents from bundle.json
    // A write test load the content from bundle.json, then writes out the content using ResourceFilter#write
    // method. The output will be compared with this file.
    //
    // 5) write_options.json
    //
    // JSON expression of FilterOptions object used for write test.
    //
    // 6) merge_base.<ext>
    //
    // Resource file used as base resource in merge test. A merge test loads bundle content from
    // bundle.json, then replace corresponding resource value in this resource file and emits the
    // result. The output will be compared with expected_merge.<ext> below.
    //
    // 7) expected_merge.<ext> (optional)
    //
    // Expected output for merge test above.
    //
    // 8) merge_options.json
    //
    // JSON expression of FilterOptions object used for merge test.
    //
    // A test of test cases are placed in folders in sequential numbers under test base path. For example,
    // AMDJS test cases are placed under data-driven-test-cases/AMDJS in test resource class path.
    //
    //   data-driven-test-cases/AMDJS/1
    //   data-driven-test-cases/AMDJS/2
    //   data-driven-test-cases/AMDJS/3
    //   ...
    //
    // To run these test cases, use a method runTest as below.
    //
    //   runTest("data-driven-test-cases/AMDJS", ResourceFilterFactory.getResourceFilter("AMDJS"), "js", StandardCharsets.UTF_8);
    //
    // The method walks through test case folder starting from 1, and stops when a next sequential number
    // is missing (therefore, test case number must be contiguous).

    /**
     * Runs a set of data driven resource filter tests for a filter.
     * 
     * @param path          Test case base path
     * @param filter        An instance of ResourceFilter used for testing
     * @param fileExtension File extension for the target format
     * @param charset       Charset used for reading/writing test resource files
     */
    private void runTest(String path, ResourceFilter filter, String fileExtension, Charset charset) throws IOException, ResourceFilterException {
        Gson gson = new Gson();
        for (int i = 1;; i++) {
            String basePath = path + "/" + i;
            ClassLoader loader = this.getClass().getClassLoader();

            // Load the content of bundle.json
            String bundlePath = basePath + "/bundle.json";
            InputStream bundleIs = loader.getResourceAsStream(bundlePath);
            if (bundleIs == null) {
                // No more test data
                break;
            }
            LanguageBundle bundle = null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(bundleIs, StandardCharsets.UTF_8))) {
                bundle = gson.fromJson(reader, LanguageBundle.class);
            }

            // parse test - requires input.<ext>
            String inputPath = basePath + "/input." + fileExtension;
            InputStream inputIs = loader.getResourceAsStream(inputPath);

            if (inputIs != null) {
                FilterOptions parseOptions = loadFilterOptions(basePath + "/parse_options.json", loader, gson);
                LanguageBundle parsedBundle = filter.parse(inputIs, parseOptions);
                compareBundles(bundle, parsedBundle, basePath + ":parse");
            }


            // write test - requires expected_write.<ext>
            String expectedWriteResult = loadContent(basePath + "/expected_write." + fileExtension, loader, charset);
            if (expectedWriteResult != null) {
                FilterOptions writeOptions = loadFilterOptions(basePath + "/write_options.json", loader, gson);
                ByteArrayOutputStream writeOs = new ByteArrayOutputStream();
                filter.write(writeOs, bundle, writeOptions);
                String writeResult = new String(writeOs.toByteArray(), charset);
                compareContent(expectedWriteResult, writeResult, basePath + ":write");
            }

            // merge test - requires merge_base.<ext> and expected_merge.<ext>
            String mergeBasePath = basePath + "/merge_base." + fileExtension;
            InputStream mergeBaseIs = loader.getResourceAsStream(mergeBasePath);
            if (mergeBaseIs != null) {
                String expectedMergeResult = loadContent(basePath + "/expected_merge." + fileExtension, loader, charset);
                if (expectedMergeResult != null) {
                    FilterOptions mergeOptions = loadFilterOptions(basePath + "/merge_options.json", loader, gson);
                    ByteArrayOutputStream mergeOs = new ByteArrayOutputStream();
                    filter.merge(mergeBaseIs, mergeOs, bundle, mergeOptions);
                    String mergeResult = new String(mergeOs.toByteArray(), charset);
                    compareContent(expectedMergeResult, mergeResult, basePath + ":merge");
                }
            }
        }
    }

    private static FilterOptions loadFilterOptions(String path, ClassLoader loader, Gson gson) throws IOException {
        InputStream is = loader.getResourceAsStream(path);
        FilterOptions filterOptions = null;
        if (is != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                filterOptions = gson.fromJson(reader, FilterOptions.class);
            }
        }
        return filterOptions;
    }

    private static String loadContent(String path, ClassLoader loader, Charset charset) throws IOException {
        InputStream is = loader.getResourceAsStream(path);
        if (is == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset))) {
            char[] buf = new char[1024];
            int len;
            while ((len = reader.read(buf)) >= 0) {
                result.append(buf, 0, len);
            }
        }

        return result.toString();
    }

    private static void compareBundles(LanguageBundle expected, LanguageBundle actual, String testDesc) {
        String msgBase = "[" + testDesc + "] ";
        Assert.assertEquals(msgBase + "Notes", expected.getNotes(), actual.getNotes());
        Assert.assertEquals(msgBase + "Metadata", expected.getMetadata(), actual.getMetadata());
        Assert.assertEquals(msgBase + "Embedded laguage code",  expected.getEmbeddedLanguageCode(), actual.getEmbeddedLanguageCode());;
        Assert.assertEquals(msgBase + "Embedded source language code", expected.getEmbeddedSourceLanguageCode(), actual.getEmbeddedSourceLanguageCode());

        // Compare resource strings
        Map<String, ResourceString> expMap = new LinkedHashMap<>();
        for (ResourceString res : expected.getResourceStrings()) {
            expMap.put(res.getKey(), res);
        }

        Set<String> actKeys = new TreeSet<>();
        for (ResourceString actResString: actual.getResourceStrings()) {
            String key = actResString.getKey();
            ResourceString expResString = expMap.get(key);
            Assert.assertNotNull(msgBase + "Extra key - " + key, expResString);
            actKeys.add(key);
            Assert.assertEquals(msgBase + "ResourceString - " + key, expResString, actResString);
        }

        Set<String> expKeys = new TreeSet<>(expMap.keySet());
        expKeys.removeAll(actKeys);
        Assert.assertTrue("Missing keys - " + expKeys, expKeys.isEmpty());
    }

    private static void compareContent(String expected, String actual, String testDesc) {
        String msgBase = "[" + testDesc + "] ";

        // compare line by line
        String[] actualLines = actual.split("\\n");
        String[] expectedLines = expected.split("\\n");

        for (int idx = 0; idx < actualLines.length && idx < expectedLines.length; idx++) {
            int lineNo = idx + 1;
            Assert.assertEquals(msgBase + "Text at line: " + lineNo, expectedLines[idx], actualLines[idx]);
        }

        Assert.assertFalse(msgBase + "Extra lines", expectedLines.length < actualLines.length);
        Assert.assertFalse(msgBase + "Missing lines", expectedLines.length > actualLines.length);
    }

    @Test
    public void testAmdJs() throws IOException, ResourceFilterException {
        runTest("data-driven-test-cases/AMDJS", ResourceFilterFactory.getResourceFilter("AMDJS"), "js", StandardCharsets.UTF_8);
    }
}
