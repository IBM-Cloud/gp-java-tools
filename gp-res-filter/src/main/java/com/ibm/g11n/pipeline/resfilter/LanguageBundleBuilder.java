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
package com.ibm.g11n.pipeline.resfilter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A builder class for {@link LanguageBundle}
 * 
 * @author yoshito_umaoka
 */
public final class LanguageBundleBuilder {
    private List<ResourceString> resourceStrings = new LinkedList<ResourceString>();
    private List<String> notes = new LinkedList<String>();
    private String embeddedLanguageCode;
    private String embeddedSourceLanguageCode;

    private final boolean autoSequenceNumbers;
    private int seqNum = 1;

    /**
     * Constructs a <code>LanguageBundleBuilder</code>.
     * 
     * @param autoSequenceNumbers   Whether a sequence number is set and incremented when
     *      {@link #addResourceString(String, String) or {@link #addResourceString(com.ibm.g11n.pipeline.resfilter.ResourceString.Builder)
     *      is called.
     */
    public LanguageBundleBuilder(boolean autoSequenceNumbers) {
        this.autoSequenceNumbers = autoSequenceNumbers;
    }

    /**
     * Adds a resource string with the specified resource key and value.
     * When <code>autoSequenceNumbers</code> is <code>true</code> in the constructor, this method
     * automatically assigns a sequence number to the resource string data.
     * 
     * @param key   The resource key.
     * @param value The resource string value.
     * @return  this builder instance.
     */
    public LanguageBundleBuilder addResourceString(String key, String value) {
        return addResourceString(ResourceString.with(key, value));
    }

    /**
     * Adds a resource string built by the specified {@link ResourceString.Builder} instance.
     * <p>
     * When <code>autoSequenceNumbers</code> is <code>true</code> in the constructor, this method
     * overwrite sequence number set in the {@link ResourceString.Builder} instance with the
     * sequential number calculated by this builder instance.
     * 
     * @param resourceStringBuilder The {@link ResourceString.Builder} instance.
     * @return  this builder instance.
     */
    public LanguageBundleBuilder addResourceString(ResourceString.Builder resourceStringBuilder) {
        if (autoSequenceNumbers) {
            resourceStringBuilder.sequenceNumber(seqNum++);
        }
        resourceStrings.add(resourceStringBuilder.build());
        return this;
    }

    /**
     * Adds a resource string with the specified key, value and sequence number.
     * <p>
     * When <code>autoSequenceNumbers</code> is <code>true</code> in the constructor, this method
     * ignore the argument <code>sequenceNumber</code>. This method sets a sequential number
     * calculated by this builder instance instead.
     * 
     * @param key   The resource key.
     * @param value The resource string value.
     * @param sequenceNumber    The sequence number of the resource string.
     * @return  this builder instance.
     */
    public LanguageBundleBuilder addResourceString(String key, String value, int sequenceNumber) {
        ResourceString.Builder resourceStringBuilder = ResourceString.with(key, value);
        if (autoSequenceNumbers) {
            resourceStringBuilder.sequenceNumber(seqNum++);
        } else {
            resourceStringBuilder.sequenceNumber(sequenceNumber);
        }
        return addResourceString(resourceStringBuilder);
    }

    /**
     * Adds a resource string with the specified key, value, sequence number and notes.
     * <p>
     * When <code>autoSequenceNumbers</code> is <code>true</code> in the constructor, this method
     * ignore the argument <code>sequenceNumber</code>. This method sets a sequential number
     * calculated by this builder instance instead.
     * 
     * @param key   The resource key.
     * @param value The resource string value.
     * @param sequenceNumber    The sequence number of the resource string/
     * @param notes The notes (comments) attached to the resource string.
     * @return  this builder instance.
     */
    public LanguageBundleBuilder addResourceString(String key, String value, int sequenceNumber, List<String> notes) {
        ResourceString.Builder resourceStringBuilder = ResourceString.with(key, value).notes(notes);
        if (autoSequenceNumbers) {
            resourceStringBuilder.sequenceNumber(seqNum++);
        } else {
            resourceStringBuilder.sequenceNumber(sequenceNumber);
        }
        return addResourceString(resourceStringBuilder);
    }

    /**
     * Add a resource string with the specified key, value, sequence number, notes and source value.
     * <p>
     * When <code>autoSequenceNumbers</code> is <code>true</code> in the constructor, this method
     * ignore the argument <code>sequenceNumber</code>. This method sets a sequential number
     * calculated by this builder instance instead.
     * 
     * @param key   The resource key.
     * @param value The resource string value.
     * @param sequenceNumber    The sequence number of the resource string/
     * @param notes The notes (comments) attached to the resource string.
     * @param sourceValue   The source resource string value.
     * @return  this builder instance.
     */
    public LanguageBundleBuilder addResourceString(String key, String value, int sequenceNumber, List<String> notes, String sourceValue) {
        ResourceString.Builder resourceStringBuilder = ResourceString.with(key, value).notes(notes).sourceValue(sourceValue);
        if (autoSequenceNumbers) {
            resourceStringBuilder.sequenceNumber(seqNum++);
        } else {
            resourceStringBuilder.sequenceNumber(sequenceNumber);
        }
        return addResourceString(resourceStringBuilder);
    }

    /**
     * Add a <code>ResourceString</code> instance.
     * <p>
     * When <code>autoSequenceNumbers</code> is <code>true</code> in the constructor, this method
     * overwrite sequence number set in the {@link ResourceString} instance with the
     * sequential number calculated by this builder instance.
     * 
     * @param resourceString    The resource string object.
     * @return  this builder instance.
     */
    public LanguageBundleBuilder addResourceString(ResourceString resourceString) {
        if (autoSequenceNumbers) {
            // TODO
        } else {
            resourceStrings.add(resourceString);
        }
        return this;
    }

    /**
     * Adds a single bundle note (comment).
     * @param note  The note to be appended.
     * @return  this builder instance.
     */
    public LanguageBundleBuilder addNote(String note) {
        notes.add(note);
        return this;
    }

    /**
     * Adds an array of bundle notes (comments).
     * @param notes The array of notes to be appended.
     * @return  this builder instance.
     */
    public LanguageBundleBuilder addNotes(List<String> notes) {
        this.notes.addAll(notes);
        return this;
    }

    /**
     * Sets the array of bundle notes (comments). Unlike {@link #addNotes(List)},
     * this method replaces previously set notes with the specified notes.
     * 
     * @param notes The array of notes to be set.
     * @return  this builder instance.
     */
    public LanguageBundleBuilder notes(List<String> notes) {
        if (notes == null) {
            this.notes.clear();;
        } else {
            this.notes = new ArrayList<>(notes);
        }
        return this;
    }

    /**
     * Sets the specified embedded language code to the bundle.
     * 
     * @param code  The language code embedded in resource contents.
     * @return  this builder instance.
     */
    public LanguageBundleBuilder embeddedLanguageCode(String code) {
        this.embeddedLanguageCode = code;
        return this;
    }

    /**
     * Sets the specified embedded source language code to the bundle.
     * 
     * @param code  The source language code embedded in resource contents.
     * @return  this builder instance.
     */
    public LanguageBundleBuilder embeddedSourceLanguageCode(String code) {
        this.embeddedSourceLanguageCode = code;
        return this;
    }

    /**
     * Returns a new instance of {@link LanguageBundle} configured by this builder.
     * 
     * @return  A new instance of {@link LanguageBundle} configured by this builder.
     */
    public LanguageBundle build() {
        LanguageBundle bundle = new LanguageBundle();
        bundle.setResourceStrings(new ArrayList<ResourceString>(resourceStrings));
        bundle.setNotes(new ArrayList<String>(notes));
        bundle.setEmbeddedLanguageCode(embeddedLanguageCode);
        bundle.setEmbeddedSourceLanguageCode(embeddedSourceLanguageCode);

        return bundle;
    }
}
