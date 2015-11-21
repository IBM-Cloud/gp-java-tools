/*  
 * Copyright IBM Corp. 2015
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
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * JSON resource filter implementation.
 * 
 * @author Yoshito Umaoka
 */
public class JsonResource implements ResourceFilter {

    @Override
    public Map<String, String> parse(InputStream inStream) throws IOException {
        Map<String, String> resultMap = new HashMap<String, String>();
        try (InputStreamReader reader = new InputStreamReader(new BomInputStream(inStream), "UTF-8")) {
            JsonElement root = new JsonParser().parse(reader);
            if (!root.isJsonObject()) {
                throw new IllegalResourceFormatException("The root JSON element is not an JSON object.");
            }
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                    throw new IllegalResourceFormatException("The value of JSON element " + key + " is not a string.");
                }
                resultMap.put(key, value.getAsString());
            }
        } catch (JsonParseException e) {
            throw new IllegalResourceFormatException("Failed to parse the specified JSON contents.", e);
        }
        return resultMap;
    }

    @Override
    public void write(OutputStream outStream, String language, Map<String, String> data) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(outStream), "UTF-8")) {
            new Gson().toJson(data, writer);
        }
    }

    @Override
    public void merge(InputStream base, OutputStream outStream, String language, Map<String, String> data) throws IOException{
        throw new UnsupportedOperationException("Merging JSON resource is not supported.");
    }
}
