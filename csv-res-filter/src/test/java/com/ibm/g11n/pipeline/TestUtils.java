/*  
 * Copyright IBM Corp. 2018
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
package com.ibm.g11n.pipeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import com.ibm.g11n.pipeline.resfilter.LanguageBundle;
import com.ibm.g11n.pipeline.resfilter.LanguageBundleBuilder;

class TestUtils {
    static LanguageBundle createLanguageBundle(TestResourceStringData[] entries) {
        LanguageBundleBuilder lbb = new LanguageBundleBuilder(false);
        for (TestResourceStringData entry : entries) {
            lbb.addResourceString(entry.key, entry.value, entry.seq);
        }
        return lbb.build();
    }

    static InputStream creteInputStream(String[] lines) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8)))) {
            for (String line : lines) {
                pw.println(line);
            }
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    static void compareLines(String[] expectedLines, byte[] actual) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(actual), StandardCharsets.UTF_8))) {
            int idx = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (idx >= expectedLines.length) {
                    break;
                }
                String expected = expectedLines[idx++];
                assertEquals("Output line " + idx, expected, line);
            }
            assertEquals("Number of output lines", expectedLines.length, idx);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
