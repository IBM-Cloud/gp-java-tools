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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import com.ibm.g11n.pipeline.resfilter.FilterInfo.Type;
import com.ibm.g11n.pipeline.resfilter.FilterOptions;
import com.ibm.g11n.pipeline.resfilter.LanguageBundle;
import com.ibm.g11n.pipeline.resfilter.LanguageBundleBuilder;
import com.ibm.g11n.pipeline.resfilter.MultiBundleResourceFilter;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterException;
import com.ibm.g11n.pipeline.resfilter.ResourceString;

public class MultiBundleCSVFilter extends MultiBundleResourceFilter {

    public static final String ID = "CSV-MULTI";
    public static final Type TYPE = Type.MULTI;

    @Override
    public Map<String, LanguageBundle> parse(InputStream inStream, FilterOptions options)
            throws IOException, ResourceFilterException {
        Map<String, LanguageBundleBuilder> builders = new HashMap<String, LanguageBundleBuilder>();
        CSVParser parser = CSVParser.parse(inStream, StandardCharsets.UTF_8,
                CSVFormat.RFC4180.withHeader("module", "key", "value").withSkipHeaderRecord(true));

        for (CSVRecord record : parser) {
            String bundle = record.get(0);
            String key = record.get(1);
            String value = record.get(2);

            LanguageBundleBuilder bundleBuilder = builders.get(bundle);
            if (bundleBuilder == null) {
                bundleBuilder = new LanguageBundleBuilder(true);
                builders.put(bundle, bundleBuilder);
            }
            bundleBuilder.addResourceString(key, value);
        }

        Map<String, LanguageBundle> result = new TreeMap<String, LanguageBundle>();
        for (Entry<String, LanguageBundleBuilder> bundleEntry : builders.entrySet()) {
            String bundleName = bundleEntry.getKey();
            LanguageBundle bundleData = bundleEntry.getValue().build();
            result.put(bundleName, bundleData);
        }

        return result;
    }

    @Override
    public void write(OutputStream outStream, Map<String, LanguageBundle> languageBundles, FilterOptions options)
            throws IOException, ResourceFilterException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_8));
        CSVPrinter printer = CSVFormat.RFC4180.withHeader("module", "key", "value").print(writer);

        // Sort by bundle
        TreeMap<String, LanguageBundle> sortedBundles = new TreeMap<>(languageBundles);

        for (Entry<String, LanguageBundle> bundleEntry: sortedBundles.entrySet()) {
            String module = bundleEntry.getKey();
            LanguageBundle languageBundle = bundleEntry.getValue();
            for (ResourceString resString : languageBundle.getSortedResourceStrings()) {
                printer.printRecord(module, resString.getKey(), resString.getValue());
            }
        }
        printer.flush();
    }

    @Override
    public void merge(InputStream baseStream, OutputStream outStream, Map<String, LanguageBundle> languageBundles,
            FilterOptions options) throws IOException, ResourceFilterException {
        // create key-value map for each bundle
        Map<String, Map<String, String>> kvMaps = new HashMap<String, Map<String, String>>();
        for (Entry<String, LanguageBundle> bundleEntry: languageBundles.entrySet()) {
            LanguageBundle languageBundle = bundleEntry.getValue();
            Map<String, String> kvMap = new HashMap<String, String>();
            for (ResourceString resString : languageBundle.getResourceStrings()) {
                kvMap.put(resString.getKey(), resString.getValue());
            }
            kvMaps.put(bundleEntry.getKey(), kvMap);
        }

        CSVParser parser = CSVParser.parse(baseStream, StandardCharsets.UTF_8,
                CSVFormat.RFC4180.withHeader("module", "key", "value").withSkipHeaderRecord(true));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_8));
        CSVPrinter printer = CSVFormat.RFC4180.withHeader("module", "key", "value").print(writer);
        for (CSVRecord record : parser) {
            String module = record.get(0);
            String key = record.get(1);
            String value = record.get(2);
            Map<String, String> moduleKVMap = kvMaps.get(module);
            if (moduleKVMap != null) {
                String trValue = moduleKVMap.get(key);
                if (trValue != null) {
                    value = trValue;
                }
            }
            printer.printRecord(module, key, value);
        }
        printer.flush();
    }
}
