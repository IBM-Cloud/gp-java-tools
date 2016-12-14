/*
 * Copyright IBM Corp. 2016
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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class Bundle {
    private List<String> notes;
    private Collection<ResourceString> resStrings;

    public Bundle () {
        this.notes = null;
        this.resStrings = null;
    }

    public Bundle(Collection<ResourceString> resStrings, List<String> notes) {
        if (resStrings != null) {
            this.resStrings = new LinkedList<ResourceString>(resStrings);
        }
        if (notes != null) {
            this.notes = new ArrayList<>(notes);
        }
    }

    public void addResourceString(ResourceString resString) {
        if (resStrings == null) {
            resStrings = new LinkedList<>();
        }
        resStrings.add(resString);
    }

    public void addResourceString(String key, String value, int sequenceNumber) {
        addResourceString(new ResourceString(key, value, sequenceNumber));
    }

    public void addNote(String note) {
        if (notes == null) {
            notes = new ArrayList<>();
        }
        notes.add(note);
    }
    
    public void addNotes(List<String> inputNotes) {
        for (String note : inputNotes) {
            notes.add(note);
        }
    }

    public Collection<ResourceString> getResourceStrings() {
        if (resStrings == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(resStrings);
    }

    public List<String> getNotes() {
        if (notes == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(notes);
    }
}
