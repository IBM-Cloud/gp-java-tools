/*
 * Copyright IBM Corp. 2015, 2017
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
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonToken;
import com.ibm.g11n.pipeline.resfilter.ResourceString.ResourceStringComparator;

/**
 * JSON resource filter implementation.
 *
 * @author Yoshito Umaoka, John Emmons
 */
public class JsonResource implements ResourceFilter {

    private class KeyPiece {
        String keyValue;
        JsonToken keyType;

        KeyPiece(String keyValue, JsonToken keyType) {
            this.keyValue = keyValue;
            this.keyType = keyType;
        }
    }

    @Override
    public Bundle parse(InputStream inStream) throws IOException {
        Bundle bundle = new Bundle();
        try (InputStreamReader reader = new InputStreamReader(new BomInputStream(inStream), StandardCharsets.UTF_8)) {
            JsonElement root = new JsonParser().parse(reader);
            if (!root.isJsonObject()) {
                throw new IllegalResourceFormatException("The root JSON element is not an JSON object.");
            }
            addBundleStrings(root.getAsJsonObject(), "", bundle, 0);
        } catch (JsonParseException e) {
            throw new IllegalResourceFormatException("Failed to parse the specified JSON contents.", e);
        }
        return bundle;
    }

    private int addBundleStrings(JsonObject obj, String keyPrefix, Bundle bundle, int sequenceNum) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                sequenceNum = addBundleStrings(value.getAsJsonObject(), modifiedKeyPrefix(keyPrefix,key,"$.") , bundle, sequenceNum);
            } else if (value.isJsonArray()) {
                JsonArray ar = value.getAsJsonArray();
                for (int i = 0; i < ar.size(); i++) {
                    JsonElement arrayEntry = ar.get(i);
                    if (arrayEntry.isJsonPrimitive() && arrayEntry.getAsJsonPrimitive().isString()) {
                        sequenceNum++;
                        bundle.addResourceString(modifiedKeyPrefix(keyPrefix,key,"$.") + "[" + Integer.toString(i) + "]",
                                arrayEntry.getAsString(), sequenceNum);

                    } else {
                        sequenceNum = addBundleStrings(arrayEntry.getAsJsonObject(),
                                modifiedKeyPrefix(keyPrefix,key,"$.") + "[" + Integer.toString(i) + "]", bundle, sequenceNum);
                    }
                }
            } else if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                throw new IllegalResourceFormatException("The value of JSON element " + key + " is not a string.");
            } else {
                sequenceNum++;
                bundle.addResourceString(modifiedKeyPrefix(keyPrefix,key,""), value.getAsString(), sequenceNum);
            }
        }
        return sequenceNum;
    }

    private String modifiedKeyPrefix( String keyPrefix, String key, String addPrefixIfEmpty) {
        
        final Pattern specialSequences = Pattern.compile("[.'\\[\\]]");
        if (key.isEmpty()) {
            return keyPrefix;
        }
        if (specialSequences.matcher(key).find(0)) {
            String modifiedKey = key.replaceAll("'", "\\\\u0027");
            return keyPrefix + "['" + modifiedKey + "']";
        } else {
            if (keyPrefix.isEmpty()) {
                return addPrefixIfEmpty + key;
            }
            return keyPrefix + "." + key;
        }
    }
    
    @Override
    public void write(OutputStream outStream, String language, Bundle bundle) throws IOException {
        // extracts key value pairs in original sequence order
        TreeSet<ResourceString> sortedResources = new TreeSet<>(new ResourceStringComparator());
        sortedResources.addAll(bundle.getResourceStrings());
        JsonObject output = new JsonObject();
        for (ResourceString res : sortedResources) {
            String key = res.getKey();
            List<KeyPiece> keyPieces = splitKeyPieces(key);
            JsonElement current = output;
            for (int i = 0; i < keyPieces.size(); i++) {
                if (i + 1 < keyPieces.size()) { // There is structure under this key piece
                    if (current.isJsonObject()) {
                        JsonObject currentObject = current.getAsJsonObject();
                        if (!currentObject.has(keyPieces.get(i).keyValue)) {
                            if (keyPieces.get(i + 1).keyType == JsonToken.BEGIN_ARRAY) {
                                currentObject.add(keyPieces.get(i).keyValue, new JsonArray());
                            } else {
                                currentObject.add(keyPieces.get(i).keyValue, new JsonObject());
                            }
                        }
                        current = currentObject.get(keyPieces.get(i).keyValue);
                    } else {
                        JsonArray currentArray = current.getAsJsonArray();
                        Integer idx = Integer.valueOf(keyPieces.get(i).keyValue);
                        for ( int arrayIndex = currentArray.size(); arrayIndex <= idx ; arrayIndex++) {
                            currentArray.add(JsonNull.INSTANCE);
                        }
                        if (currentArray.get(idx).isJsonNull()) {
                            if (keyPieces.get(i + 1).keyType == JsonToken.BEGIN_ARRAY) {
                                currentArray.set(idx, new JsonArray());
                            } else {
                                currentArray.set(idx, new JsonObject());
                            }
                        } 
                        current = currentArray.get(idx);
                    }
                } else { // This is the leaf node
                    if (keyPieces.get(i).keyType == JsonToken.BEGIN_ARRAY) {
                        JsonArray currentArray = current.getAsJsonArray();
                        Integer idx = Integer.valueOf(keyPieces.get(i).keyValue);
                        JsonPrimitive e = new JsonPrimitive(res.getValue());
                        for ( int arrayIndex = currentArray.size(); arrayIndex <= idx ; arrayIndex++) {
                            currentArray.add(JsonNull.INSTANCE);
                        }
                        current.getAsJsonArray().set(idx, e);
                    } else {
                        current.getAsJsonObject().addProperty(keyPieces.get(i).keyValue, res.getValue());
                    }
                }
            }
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(outStream),
                StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(output, writer);
        }
    }

    private List<KeyPiece> splitKeyPieces(String key) {
        List<KeyPiece> result = new ArrayList<KeyPiece>();
        Matcher onlyDigits = Pattern.compile("^\\d+$").matcher("");
        // Disregard $. at the beginning - it's not really part of the key...
        List<String> tokens = findTokens(key.startsWith("$.") ? key.substring(2) : key);
        for (String s : tokens) {
            if (s.startsWith("'")) {
                // Turn any "\u0027" in the key back into '
                String modifiedKeyPiece = s.substring(1, s.length() - 1).replaceAll("\\\\u0027", "'");
                result.add(new KeyPiece(modifiedKeyPiece, JsonToken.BEGIN_OBJECT));
            } else if (onlyDigits.reset(s).matches()) {
                result.add(new KeyPiece(s, JsonToken.BEGIN_ARRAY));
            } else {
                for (String s2 : s.split("\\.")) {
                    if (!s2.isEmpty()) {
                        result.add(new KeyPiece(s2, JsonToken.BEGIN_OBJECT));
                    }
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static List<String> findTokens(String data) {
        List<String> tokens = new ArrayList<String>();
        boolean inQuotes = false;
        StringBuilder currentToken = new StringBuilder();
        StringCharacterIterator i = new StringCharacterIterator(data);
        while (i.current() != StringCharacterIterator.DONE) {
            char c = i.current();
            if ( c == '\'' ) {
                inQuotes = !inQuotes;
            }
            if (!inQuotes && ( c == '.' || c == '[' || c == ']')) {
                tokens.add(currentToken.toString());
                currentToken.setLength(0);
            } else {
                currentToken.append(c);
            }
            i.next();
        }
        tokens.add(currentToken.toString());
        return Collections.unmodifiableList(tokens);
    }

    @Override
    public void merge(InputStream base, OutputStream outStream, String language, Bundle bundle) throws IOException {
        // TODO: Add merge implementation here. For now, fallback to write()
        // operation.
        write(outStream, language, bundle);
    }
}
