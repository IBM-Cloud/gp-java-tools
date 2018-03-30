/*
 * Copyright IBM Corp. 2016, 2018
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

/**
 * <code>ResourceString</code> class stores a single resource string value
 * and other meta data.
 * 
 * @author parth, yoshito_umaoka
 */
public final class ResourceString {

    public static int DEFAULT_SEQUENCE_NUMBER = -1;

    private String key;
    private String value;
    private String sourceValue;
    private int sequenceNumber;
    private List<String> notes;
    private Map<String, String> metadata;

    /**
     * A convenient builder for <code>ResourceString</code>
     * @author yoshito_umaoka
     */
    public static class Builder {
        private String key;
        private String value;
        private String sourceValue;
        private int sequenceNumber = DEFAULT_SEQUENCE_NUMBER;
        private List<String> notes;
        private Map<String, String> metadata;

        private Builder(String key, String value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Sets the source value of this resource string.
         * @param sourceValue   The source value of the resource string.
         * @return  this builder.
         */
        public Builder sourceValue(String sourceValue) {
            this.sourceValue = sourceValue;
            return this;
        }

        /**
         * Sets the sequence number of this resource string.
         * @param sequenceNumber    The sequence number of the resource string.
         * @return this builder.
         */
        public Builder sequenceNumber(int sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
            return this;
        }

        /**
         * Sets the array of notes for this resource string.
         * @param notes The array of notes.
         * @return  this builder.
         */
        public Builder notes(List<String> notes) {
            if (notes == null) {
                this.notes = null;
            } else {
                this.notes = new ArrayList<String>(notes);
            }
            return this;
        }

        /**
         * Adds the single note for this resource string. If any notes were already
         * added before, the note will be appended to the end of the array.
         * @param note  The single note.
         * @return  this builder.
         */
        public Builder addNote(String note) {
            if (this.notes == null) {
                this.notes = new ArrayList<String>();
            }
            this.notes.add(note);
            return this;
        }

        /**
         * Sets the metadata map for this resource string.
         * @param metadata  The metadata key-value pairs.
         * @return  this builder
         */
        public Builder metadata(Map<String, String> metadata) {
            if (metadata == null) {
                this.metadata = null;
            } else {
                this.metadata = new TreeMap<>(metadata);
            }
            return this;
        }

        /**
         * Adds a single key-value pair to the resource string metadata. If the key
         * was already set, the value will be updated.
         * 
         * @param key   The metadata key.
         * @param value The metadata value.
         * @return  this builder.
         */
        public Builder addMetadata(String key, String value) {
            if (this.metadata == null) {
                this.metadata = new TreeMap<>();
            }
            this.metadata.put(key, value);
            return this;
        }

        /**
         * Builds an instance of {@link ResourceString}.
         * @return  an instance of {@link ResourceString}.
         */
        public ResourceString build() {
            if (key == null || value == null) {
                throw new NullPointerException("Both key(" + key + ") and value(" + value
                        + ") must be non-null.");
            }
            return new ResourceString(this);
        }
    }

    private ResourceString(Builder builder) {
        this.key = builder.key;
        this.value = builder.value;
        this.sourceValue = builder.sourceValue;
        this.sequenceNumber = builder.sequenceNumber;
        this.notes = builder.notes;
        this.metadata = builder.metadata;
    }

    /**
     * Convenient method for creating a new {@link Builder} with the specified
     * key and value.
     * @param key   The resource key.
     * @param value The resource value.
     * @return  an instance of new {@link Builder}.
     */
    public static Builder with(String key, String value) {
        return new Builder(key, value);
    }

    /**
     * Returns the resource key.
     * @return  the resource key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the resource value.
     * @return  the resource value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the resource's source value.
     * @return  the reources's source value.
     */
    public String getSourceValue() {
        return sourceValue;
    }

    /**
     * Returns the sequence number of this resource string.
     * @return  the sequence number of this resource string.
     */
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Returns an unmodifiable list of notes for this resource string.
     * An empty list is returned when no notes are available.
     * @return  an unmodifiable list of notes for this resource string.
     */
    public List<String> getNotes() {
        if (notes == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(notes);
    }

    /**
     * Returns an unmodifiable map of metadata key-value pairs.
     * An empty map is returned when no metadata entries are available.
     * @return  an unmodifiable map of metadata key-value pairs.
     */
    public Map<String, String> getMetadata() {
        if (metadata == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(metadata);
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof ResourceString)) {
            return false;
        }
        ResourceString rs = (ResourceString) obj;
        return Objects.equals(this.key, rs.key)
                && Objects.equals(this.value, rs.value)
                && Objects.equals(this.sourceValue, rs.sourceValue)
                && Objects.equals(this.notes, rs.notes)
                && Objects.equals(this.metadata, rs.metadata)
                && this.sequenceNumber == rs.sequenceNumber;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuilder builder = new StringBuilder("{");
        builder.append("key: ").append(key);
        builder.append(", value: ").append(value);
        builder.append(", sourceValue: ").append(sourceValue);
        builder.append(", notes: ").append(notes);
        builder.append(", metadata: ").append(metadata);
        builder.append(", sequenceNumber: ")
            .append(sequenceNumber == DEFAULT_SEQUENCE_NUMBER ? "<default>" : sequenceNumber);
        builder.append("}");
        return builder.toString();
    }

    /**
     * Comparator implementation for {@link ResourceString}
     * 
     * @author yoshito_umaoka
     */
    public static class ResourceStringComparator implements Comparator<ResourceString> {
        private boolean isUnknownSequenceFirst = false;

        /**
         * Default constructor, equivalent to <code>ResourceStringComparator(false)</code>.
         */
        public ResourceStringComparator() {
            this(false);
        }

        /**
         * Constructor.
         * @param isUnknownSequenceFirst    When true, a resource string with default
         *  sequence number value will be less than another resource string with non-default
         *  sequence number.
         */
        public ResourceStringComparator(boolean isUnknownSequenceFirst) {
            this.isUnknownSequenceFirst = isUnknownSequenceFirst;
        }

        @Override
        /**
         * {@inheritDoc}
         */
        public int compare(ResourceString o1, ResourceString o2) {
            int seq1 = o1.getSequenceNumber();
            int seq2 = o2.getSequenceNumber();

            if (seq1 < 0) {
                if (seq2 > 0) {
                    // only seq2 is known
                    return isUnknownSequenceFirst ? -1 : 1;
                }
            } else if (seq2 < 0) {
                // only seq1 is known
                return isUnknownSequenceFirst ? 1 : -1;
            } else {
                // both sequence are available
                if (seq1 < seq2) {
                    return -1;
                } else if (seq1 > seq2) {
                    return 1;
                }
            }

            // Either sequence values are same or both sequence value are not
            // available.
            // Use key value's natural order as tie-breaker.
            int cmp = compareStrings(o1.getKey(), o2.getKey());
            if (cmp == 0) {
                // Use value's natural order as tie-breaker
                cmp = compareStrings(o1.getValue(), o2.getValue());
                if (cmp == 0) {
                    // Use source value's natural order as tie-breaker
                    cmp = compareStrings(o1.getSourceValue(), o2.getSourceValue());
                    if (cmp == 0) {
                        // Note value's natural order as tie-breaker
                        cmp = compareNotes(o1.getNotes(), o2.getNotes());
                        if (cmp == 0) {
                            // metadata values' natural order as tie-breaker
                            cmp = compareMetadata(o1.getMetadata(), o2.getMetadata());
                        }
                    }
                }
            }

            return cmp;
        }

        private static int compareStrings(String s1, String s2) {
            // null as lowest value
            if (s1 == null) {
                if (s2 == null) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (s2 == null) {
                return 1;
            }
            return s1.compareTo(s2);
        }

        private static int compareNotes(List<String> n1, List<String> n2) {
            // null as lowest value
            if (n1 == null) {
                if (n2 == null) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (n2 == null) {
                return 1;
            }
            int cmp = 0;
            int index = 0;
            while (cmp == 0 && index < n1.size()) {
                String s1 = n1.get(index);
                String s2 = index < n2.size() ? n2.get(index) : null;
                cmp = compareStrings(s1, s2);
                index++;
            }
            return cmp;
        }

        private static int compareMetadata(Map<String, String> md1, Map<String, String> md2) {
            if (md1 == null) {
                if (md2 == null) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (md2 == null) {
                return 1;
            }

            for (Entry<String, String> e1 : md1.entrySet()) {
                String key = e1.getKey();
                String val1 = e1.getValue();
                String val2 = md2.get(key);
                int cmp = compareStrings(val1, val2);
                if (cmp != 0) {
                    return cmp;
                }
            }
            // At this point, md2 contains all keys found in md1, and identical value for each.
            if (md2.size() > md1.size()) {
                return -1;
            }
            return 0;
        }
    }
}
