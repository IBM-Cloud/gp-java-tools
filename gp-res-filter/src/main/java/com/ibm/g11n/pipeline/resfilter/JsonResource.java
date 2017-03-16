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
import java.util.Map;
import java.util.TreeSet;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
            addBundleStrings(root.getAsJsonObject(),"", bundle, 0);
        } catch (JsonParseException e) {
            throw new IllegalResourceFormatException("Failed to parse the specified JSON contents.", e);
        }
        return bundle;
    }

    private int addBundleStrings( JsonObject obj, String keyPrefix, Bundle bundle, int sequenceNum) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                String newKeyPrefix;
                if (keyPrefix.isEmpty()) {
                    newKeyPrefix = "$." + key + ".";
                } else {
                    newKeyPrefix = keyPrefix + key + ".";
                }
                sequenceNum = addBundleStrings(value.getAsJsonObject(),newKeyPrefix,bundle,sequenceNum);
            } else if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                throw new IllegalResourceFormatException("The value of JSON element " + key + " is not a string.");
            } else {
                sequenceNum++;
                bundle.addResourceString(keyPrefix+key, value.getAsString(), sequenceNum);
            }
        }
        return sequenceNum;
    }
    @Override
    public void write(OutputStream outStream, String language, Bundle bundle) throws IOException {
        // extracts key value pairs in original sequence order
        TreeSet<ResourceString> sortedResources = new TreeSet<>(new ResourceStringComparator());
        sortedResources.addAll(bundle.getResourceStrings());
        JsonObject output = new JsonObject();
        for (ResourceString res : sortedResources) {
            String key = res.getKey();
            if (key.startsWith("$.")) {
                key = key.substring(2);
            }
            String[] keyPieces = key.split("\\.");
            JsonObject current = output;
            for (int i = 0 ; i < keyPieces.length ; i++ ) {
                if ( i + 1 < keyPieces.length ) { // There is structure under this key piece
                    if (!current.has(keyPieces[i])) {
                        current.add(keyPieces[i],new JsonObject());
                    }
                    current = current.getAsJsonObject(keyPieces[i]);
                } else { // This is the leaf node
                    current.addProperty(keyPieces[i], res.getValue());
                }
            }
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(outStream),
                StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(output, writer);
        }
    }

    @Override
    public void merge(InputStream base, OutputStream outStream, String language, Bundle bundle)
            throws IOException {
        //TODO: Add merge implementation here. For now, fallback to write() operation.
        write(outStream, language, bundle);
    }
}
