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

/**
 * @author parth
 *
 */

public class ResourceString {
    private final String key;
    private final String value;
    private int sequenceNumber;

    public ResourceString(String key, String value, int sequenceNumber) {
        this.key = key;
        this.value = value;
        this.sequenceNumber = sequenceNumber;
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

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != ResourceString.class) return false;
        ResourceString rs = (ResourceString) obj;
        return getKey().equals(rs.getKey()) && getValue().equals(rs.getValue())
                && getSequenceNumber() == rs.getSequenceNumber();
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
            String key1 = o1.getKey();
            String key2 = o2.getKey();

            // Note: key must not be null for valid ResoruceString. This
            // implementation
            // uses null key as lowest value.
            // Also two keys must not be same in a valid ResourceString
            // collection.
            if (key1 == null) {
                if (key2 == null) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (key2 == null) {
                return 1;
            }

            return key1.compareTo(key2);
        }
    }
}
