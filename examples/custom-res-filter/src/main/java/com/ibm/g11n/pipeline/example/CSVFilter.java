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
package com.ibm.g11n.pipeline.example;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import com.ibm.g11n.pipeline.resfilter.FilterInfo.Type;
import com.ibm.g11n.pipeline.resfilter.FilterOptions;
import com.ibm.g11n.pipeline.resfilter.LanguageBundle;
import com.ibm.g11n.pipeline.resfilter.LanguageBundleBuilder;
import com.ibm.g11n.pipeline.resfilter.ResourceFilter;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterException;
import com.ibm.g11n.pipeline.resfilter.ResourceString;

public class CSVFilter extends ResourceFilter {

    public static final String ID = "CSV";
    public static final Type TYPE = Type.SINGLE;

    @Override
    public LanguageBundle parse(InputStream inStream, FilterOptions options)
            throws IOException, ResourceFilterException {
        LanguageBundleBuilder bundleBuilder = new LanguageBundleBuilder(true);
        CSVParser parser = CSVParser.parse(inStream, StandardCharsets.UTF_8,
                CSVFormat.RFC4180.withHeader("key", "value").withSkipHeaderRecord(true));
        for (CSVRecord record : parser) {
            String key = record.get(0);
            String value = record.get(1);
            bundleBuilder.addResourceString(key, value);
        }
        return bundleBuilder.build();
    }

    @Override
    public void write(OutputStream outStream, LanguageBundle languageBundle, FilterOptions options)
            throws IOException, ResourceFilterException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_8));
        CSVPrinter printer = CSVFormat.RFC4180.withHeader("key", "value").print(writer);
        for (ResourceString resString : languageBundle.getSortedResourceStrings()) {
            printer.printRecord(resString.getKey(), resString.getValue());
        }
        printer.flush();
    }

    @Override
    public void merge(InputStream baseStream, OutputStream outStream, LanguageBundle languageBundle,
            FilterOptions options) throws IOException, ResourceFilterException {
        // create key-value map
        Map<String, String> kvMap = new HashMap<String, String>();
        for (ResourceString resString : languageBundle.getResourceStrings()) {
            kvMap.put(resString.getKey(), resString.getValue());
        }

        CSVParser parser = CSVParser.parse(baseStream, StandardCharsets.UTF_8,
                CSVFormat.RFC4180.withHeader("key", "value").withSkipHeaderRecord(true));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_8));
        CSVPrinter printer = CSVFormat.RFC4180.withHeader("key", "value").print(writer);
        for (CSVRecord record : parser) {
            String key = record.get(0);
            String value = record.get(1);
            String trValue = kvMap.get(key);
            if (trValue != null) {
                value = trValue;
            }
            printer.printRecord(key, value);
        }

        write(outStream,languageBundle,options);
    }
}
