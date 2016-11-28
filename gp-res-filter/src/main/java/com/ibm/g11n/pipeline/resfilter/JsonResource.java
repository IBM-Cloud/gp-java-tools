/*
 * Copyright IBM Corp. 2015, 2016
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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.ibm.g11n.pipeline.resfilter.ResourceString.ResourceStringComparator;

/**
 * JSON resource filter implementation.
 *
 * @author Yoshito Umaoka
 */
public class JsonResource implements ResourceFilter {

    @Override
    public Bundle parse(InputStream inStream) throws IOException {
        Bundle bundle = new Bundle();
        try (InputStreamReader reader = new InputStreamReader(new BomInputStream(inStream), StandardCharsets.UTF_8)) {
            JsonElement root = new JsonParser().parse(reader);
            if (!root.isJsonObject()) {
                throw new IllegalResourceFormatException("The root JSON element is not an JSON object.");
            }
            int sequenceNum = 0;
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                    throw new IllegalResourceFormatException("The value of JSON element " + key + " is not a string.");
                }
                sequenceNum++;
                bundle.addResourceString(key, value.getAsString(), sequenceNum);
            }
        } catch (JsonParseException e) {
            throw new IllegalResourceFormatException("Failed to parse the specified JSON contents.", e);
        }
        return bundle;
    }

    @Override
    public void write(OutputStream outStream, String language, Bundle bundle) throws IOException {
        // extracts key value pairs in original sequence order
        TreeSet<ResourceString> sortedResources = new TreeSet<>(new ResourceStringComparator());
        sortedResources.addAll(bundle.getResourceStrings());
        LinkedHashMap<String, String> kvmap = new LinkedHashMap<>(sortedResources.size());
        for (ResourceString res : sortedResources) {
            kvmap.put(res.getKey(), res.getValue());
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(outStream),
                StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(kvmap, writer);
        }
    }

    @Override
    public void merge(InputStream base, OutputStream outStream, String language, Bundle bundle)
            throws IOException {
        throw new UnsupportedOperationException("Merging JSON resource is not supported.");
    }
}
