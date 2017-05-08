/*
 * Copyright IBM Corp. 2017
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * globalizejs resource filter implementation.
 *
 * @author John Emmons
 */
public class GlobalizeJsResource extends JsonResource {

    @Override
    public Bundle parse(InputStream inStream) throws IOException {
        Bundle bundle = new Bundle();
        try (InputStreamReader reader = new InputStreamReader(new BomInputStream(inStream), StandardCharsets.UTF_8)) {
            JsonElement root = new JsonParser().parse(reader);
            if (!root.isJsonObject()) {
                throw new IllegalResourceFormatException("The root JSON element is not a JSON object.");
            }
            JsonObject root_obj = root.getAsJsonObject();
            Set<Map.Entry<String, JsonElement>> root_elements = root_obj.entrySet();
            if (root_elements.size() != 1) {
                throw new IllegalResourceFormatException(
                        "Only one top level language tag element is allowed per file.");
            }
            Map.Entry<String, JsonElement> top_level = root_elements.iterator().next();
            String language = top_level.getKey();
            // We just hang on to the language tag as part of the bundle.
            // When doing an import, we can validate that the language tag matches what they
            // say they are importing.
            bundle.setLanguage(language);
            JsonElement language_obj = top_level.getValue();
            if (!language_obj.isJsonObject()) {
                throw new IllegalResourceFormatException("The top level language element is not a JSON object.");
            }
            addBundleStrings(language_obj.getAsJsonObject(), "", bundle, 0);
        } catch (JsonParseException e) {
            throw new IllegalResourceFormatException("Failed to parse the specified JSON contents.", e);
        }
        return bundle;
    }

    /**
     * The override of addBundleStrings is necessary because globalizejs treats arrays of strings
     * as a single long string, with each piece separated by a space, rather than treating it like
     * a real JSON array.
     */
    @Override
    protected int addBundleStrings(JsonObject obj, String keyPrefix, Bundle bundle, int sequenceNum) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                sequenceNum = addBundleStrings(value.getAsJsonObject(), modifiedKeyPrefix(keyPrefix, key, "$."), bundle,
                        sequenceNum);
            } else if (value.isJsonArray()) {
                JsonArray ar = value.getAsJsonArray();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < ar.size(); i++) {
                    JsonElement arrayEntry = ar.get(i);
                    if (arrayEntry.isJsonPrimitive() && arrayEntry.getAsJsonPrimitive().isString()) {
                        if (i > 0) {
                            sb.append(" "); //
                        }
                        sb.append(arrayEntry.getAsString());
                    } else {
                        throw new IllegalResourceFormatException(
                                "Arrays must contain only strings in a globalizejs resource.");
                    }
                }
                sequenceNum++;
                bundle.addResourceString(modifiedKeyPrefix(keyPrefix, key, ""), sb.toString(), sequenceNum);
            } else if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                throw new IllegalResourceFormatException("The value of JSON element " + key + " is not a string.");
            } else {
                sequenceNum++;
                bundle.addResourceString(modifiedKeyPrefix(keyPrefix, key, ""), value.getAsString(), sequenceNum);
            }
        }
        return sequenceNum;
    }
}
