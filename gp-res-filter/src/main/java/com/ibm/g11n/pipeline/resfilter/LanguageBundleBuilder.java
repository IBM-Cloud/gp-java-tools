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
 * @author yoshito_umaoka
 */
public final class LanguageBundleBuilder {
    private List<ResourceString> resourceStrings = new LinkedList<ResourceString>();
    private List<String> notes = new LinkedList<String>();
    private String embeddedLanguageCode;

    private final boolean autoSequenceNumbers;
    private int seqNum = 1;

    public LanguageBundleBuilder(boolean autoSequenceNumbers) {
        this.autoSequenceNumbers = autoSequenceNumbers;
    }

    public LanguageBundleBuilder addResourceString(String key, String value) {
        return addResourceString(ResourceString.with(key, value));
    }

    public LanguageBundleBuilder addResourceString(ResourceString.Builder resourceStringBuilder) {
        if (autoSequenceNumbers) {
            resourceStringBuilder.sequenceNumber(seqNum++);
        }
        resourceStrings.add(resourceStringBuilder.build());
        return this;
    }

    public LanguageBundleBuilder addResourceString(String key, String value, int sequenceNumber) {
        if (autoSequenceNumbers) {
            throw new IllegalStateException("This method is not supported in auto-sequence-number mode.");
        }
        return addResourceString(ResourceString.with(key, value).sequenceNumber(sequenceNumber));
    }

    public LanguageBundleBuilder addResourceString(String key, String value, int sequenceNumber, List<String> notes) {
        if (autoSequenceNumbers) {
            throw new IllegalStateException("This method is not supported in auto-sequence-number mode.");
        }
        return addResourceString(ResourceString.with(key, value).sequenceNumber(sequenceNumber).notes(notes));
    }

    public LanguageBundleBuilder addResourceString(String key, String value, int sequenceNumber, List<String> notes, String sourceValue) {
        if (autoSequenceNumbers) {
            throw new IllegalStateException("This method is not supported in auto-sequence-number mode.");
        }
        return addResourceString(ResourceString.with(key, value).sequenceNumber(sequenceNumber).notes(notes).sourceValue(sourceValue));
    }

    public LanguageBundleBuilder addResourceString(ResourceString resourceString) {
        if (autoSequenceNumbers) {
            throw new IllegalStateException("This method is not supported in auto-sequence-number mode.");
        }
        resourceStrings.add(resourceString);
        return this;
    }

    public LanguageBundleBuilder addNote(String note) {
        notes.add(note);
        return this;
    }

    public LanguageBundleBuilder addNotes(List<String> notes) {
        this.notes.addAll(notes);
        return this;
    }

    public LanguageBundleBuilder embeddedLanguageCode(String code) {
        this.embeddedLanguageCode = code;
        return this;
    }

    public LanguageBundle build() {
        LanguageBundle bundle = new LanguageBundle();
        bundle.setResourceStrings(new ArrayList<ResourceString>(resourceStrings));
        bundle.setNotes(new ArrayList<String>(notes));
        bundle.setEmbeddedLanguageCode(embeddedLanguageCode);

        return bundle;
    }
}
