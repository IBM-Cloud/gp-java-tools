/*  
 * Copyright IBM Corp. 2016, 2017
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
package com.ibm.g11n.pipeline.maven;

/**
 * Output content generation methods.
 * 
 * @author Yoshito Umaoka
 */
public enum OutputContentOption {
    /**
     * Merges translated resources into source file contents
     * if possible. If the output resource format does not support
     * this option, {@link #TRANSLATED_WITH_FALLBACK} is used.
     */
    MERGE_TO_SOURCE,

    /**
     * Exports translated resources. If translation is not available
     * for a resource key, the value from the source bundle is used.
     */
    TRANSLATED_WITH_FALLBACK,

    /**
     * Exports only translated resources.
     */
    TRANSLATED_ONLY,

    /**
     * Merges translated resources marked as reviewed into source
     * file contents if possible. If the output resource format
     * does not support this option, {@link #REVIEWD_WITH_FALLBACK}
     * is used.
     */
    MERGE_REVIEWED_TO_SOURCE,

    /**
     * Exports translated resources marked as reviewed. If translation
     * is not available or, not marked as reviewed, the value from the
     * source bundle is used.
     */
    REVIEWD_WITH_FALLBACK,

    /**
     * Exports only translated resources marked as reviewed.
     */
    REVIEWED_ONLY
}
