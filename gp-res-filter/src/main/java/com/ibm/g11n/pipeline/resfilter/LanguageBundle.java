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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.ibm.g11n.pipeline.resfilter.ResourceString.ResourceStringComparator;

/**
 * <code>LanguageBundle</code> is a class representing a bundle data for a language.
 * 
 * @author yoshito_umaoka
 */
public final class LanguageBundle {
    private Collection<ResourceString> resourceStrings;
    private List<String> notes;
    private String embeddedLanguageCode;
    private String embeddedSourceLanguageCode;

    /**
     * No-arg constructor.
     */
    public LanguageBundle() {
    }

    /**
     * Sets the array of notes for this bundle.
     * @param notes the array of notes for this bundle.
     */
    public void setNotes(List<String> notes) {
        this.notes = new ArrayList<>(notes);
    }

    /**
     * Returns an unmodifiable list of notes for this language bundle.
     * An empty list is returned when no notes are available.
     * @return  an unmodifiable list of notes for this language bundle.
     */
    public List<String> getNotes() {
        if (notes == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(notes);
    }

    /**
     * Sets a collection of resource strings.
     * @param resourceStrings   A set of resource strings.
     */
    public void setResourceStrings(Collection<ResourceString> resourceStrings) {
        this.resourceStrings = new ArrayList<>(resourceStrings);
    }

    /**
     * Returns an unmodifiable collection of resource strings.
     * @return  an unmodifiable collection of resource strings.
     */
    public Collection<ResourceString> getResourceStrings() {
        return Collections.unmodifiableCollection(resourceStrings);
    }

    /**
     * Sets a language code embedded in resource data.
     * <p>
     * Note: There are some resource formats that embed resource string language in resource
     * content body. For example, GlobalizeJS uses locale such as "en" "fr".. as the first level
     * key in resource object.
     * @param code  A language code embedded in resource data.
     */
    public void setEmbeddedLanguageCode(String code) {
        this.embeddedLanguageCode = code;
    }

    /**
     * Returns a language code embedded in resource data or null if
     * not available.
     * @return  a language cod embedded in resource data.
     */
    public String getEmbeddedLanguageCode() {
        return embeddedLanguageCode;
    }

    /**
     * Sets a source language code embedded in resource data.
     * <p>
     * Note: A translated XLIFF file must specify both <code>srcLang</code> and <code>trgLang</code>
     * in &lt;xliff&gt; element. This property is used when a resource format requires translation
     * source language to be included in the resource content body.
     * @param code  A source language code embedded in resource data.
     */
    public void setEmbeddedSourceLanguageCode(String code) {
        this.embeddedSourceLanguageCode = code;
    }

    /**
     * Returns a source language code embedded in resource data or null if
     * not available.
     * @return  a source language cod embedded in resource data.
     */
    public String getEmbeddedSourceLanguageCode() {
        return embeddedSourceLanguageCode;
    }

    /**
     * Returns a sorted list of {@link ResourceString}s. The returned list
     * is modifiable.
     * @return  a sorted lits of {@link ResourceString}s.
     */
    public List<ResourceString> getSortedResourceStrings() {
        List<ResourceString> sortedResStrings = new ArrayList<>(resourceStrings);
        Collections.sort(sortedResStrings, new ResourceStringComparator());
        return sortedResStrings;
    }
}

