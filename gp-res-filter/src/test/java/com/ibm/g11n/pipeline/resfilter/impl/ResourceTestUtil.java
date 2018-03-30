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
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * @author farhan, JCEmmons
 *
 */
public class ResourceTestUtil {

    /**
     * Returns true if the two files match exactly. If n is set to a number > 0,
     * the first n lines will not be compared. This is useful when the real
     * content starts after a few line, such as in a .properties file created
     * with Java. There is a comment line at the beginning which should be
     * ignored while comparing files.
     */
    public static boolean compareFiles(File expected, File actual, int n) throws FileNotFoundException, IOException {
        try (BufferedReader expectedRdr = new BufferedReader(new FileReader(expected));
                BufferedReader actualRdr = new BufferedReader(new FileReader(actual))) {

            String expectedLine;
            String actualLine;
            int lineNum = 0;
            while ((expectedLine = expectedRdr.readLine()) != null) {
                actualLine = actualRdr.readLine();

                lineNum++;

                if (n > 0) {
                    n--;
                    continue;
                }

                if (!expectedLine.equals(actualLine)) {
                    fail("Comparing file <" + actual.getAbsolutePath() + "> with file <" + expected.getAbsolutePath()
                            + "> ..." + "\nContent differs on line " + lineNum + "\nExpected content: \n"
                            + fileToString(expected) + "\nActual content:\n" + fileToString(actual) + "\n");
                    return false;
                }
            }

            // actualRdr should be at the end as well, if it's not,
            // it means the actual file has extra content at the end
            if ((actualLine = actualRdr.readLine()) != null) {
                fail("Comparing file <" + actual.getAbsolutePath() + "> with file <" + expected.getAbsolutePath()
                        + "> ..." + "\n" + "File <" + actual.getAbsolutePath()
                        + "> contains extra lines. \nExpected content: \n" + fileToString(expected)
                        + "\nActual content:\n" + fileToString(actual) + "\n");
            }
        }

        return true;
    }

    /**
     * Returns true if the two files match exactly up to the number of lines
     * specified in n
     */
    public static boolean compareFilesUpTo(File expected, File actual, int n)
            throws FileNotFoundException, IOException {
        try (BufferedReader expectedRdr = new BufferedReader(new FileReader(expected));
                BufferedReader actualRdr = new BufferedReader(new FileReader(actual))) {

            String expectedLine;
            String actualLine;
            int lineNum = 0;
            while ((expectedLine = expectedRdr.readLine()) != null && lineNum < n) {
                actualLine = actualRdr.readLine();

                lineNum++;

                if (!expectedLine.equals(actualLine)) {
                    fail("Comparing file <" + actual.getAbsolutePath() + "> with file <" + expected.getAbsolutePath()
                            + "> ..." + "\nContent differs on line " + lineNum + "\nExpected content: \n"
                            + fileToString(expected) + "\nActual content:\n" + fileToString(actual) + "\n");
                    return false;
                }
            }

        }

        return true;
    }

    /**
     * Returns true if the two files match exactly.
     */
    public static boolean compareFiles(File expected, File actual) throws FileNotFoundException, IOException {
        return compareFiles(expected, actual, 0);
    }

    /**
     * Returns the content of the file as a String.
     *
     * @param file
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static String fileToString(File file) throws FileNotFoundException, IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(file.getPath()));
        return new String(encoded, "UTF-8");
    }

    /**
     * @param expectedWriteFile
     * @param tempFile
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static void compareFilesJson(File expectedWriteFile, File tempFile)
            throws FileNotFoundException, IOException {
        JsonElement expected = parseJson(expectedWriteFile);
        JsonElement actual = parseJson(tempFile);
        assertEquals("JSON mismatch: " + tempFile.getName() + " did not match " + expectedWriteFile.getName(), expected,
                actual);
    }

    public static JsonElement parseJson(final File f) throws FileNotFoundException, IOException {
        try (final Reader reader = new FileReader(f)) {
            JsonElement parse = new JsonParser().parse(reader);
            return parse;
        }
    }
}
