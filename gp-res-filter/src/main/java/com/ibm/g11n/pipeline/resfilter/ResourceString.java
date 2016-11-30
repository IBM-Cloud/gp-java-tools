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

import java.util.Comparator;
import java.util.Objects;

/**
 * @author parth
 *
 */

public final class ResourceString {
    private final String note;
    private final String key;
    private final String value;
    private int sequenceNumber;

    public ResourceString(String note, String key, String value, int sequenceNumber) {
        this.note = note;
        this.key = key;
        this.value = value;
        this.sequenceNumber = sequenceNumber;
    }
    
    public ResourceString(String key, String value, int sequenceNumber) {
        this(null, key, value, sequenceNumber);
    }

    public ResourceString(String key, String value) {
        this(key, value, -1);
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getNote() {
        return note;
    }
    
    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ResourceString)) {
            return false;
        }
        ResourceString rs = (ResourceString) obj;
        return Objects.equals(this.key, rs.key)
                && Objects.equals(this.value, rs.value)
                && Objects.equals(this.note, rs.note)
                && this.sequenceNumber == rs.sequenceNumber;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("#");
        builder.append(getSequenceNumber());
        builder.append(" Key=");
        builder.append(getKey());
        builder.append(" Value=");
        builder.append(getValue());
        builder.append(" Note=");
        builder.append(getNote());
        return builder.toString();
    }

    public static class ResourceStringComparator implements Comparator<ResourceString> {
        private boolean isUnknownSequenceFirst = false;

        public ResourceStringComparator() {
            this(false);
        }

        public ResourceStringComparator(boolean isUnknownSequenceFirst) {
            this.isUnknownSequenceFirst = isUnknownSequenceFirst;
        }

        @Override
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
                    // Note value's natural order as tie-breaker
                    cmp = compareStrings(o1.getNote(), o2.getNote());
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
    }
}
