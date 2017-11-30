/*  
 * Copyright IBM Corp. 2015,2016
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
package com.ibm.g11n.pipeline.tools.cli;

import com.ibm.icu.lang.UCharacter;

public final class ApproximateMatcher {
    private String text;
    private String processedText;

    public ApproximateMatcher(String text) {
        this.text = text;
        this.processedText = processText(text);
    }

    private static String processText(String text) {
        // fold case
        text = UCharacter.foldCase(text, true);

        // replace characters other than letter and number
        // with space and trim.
        return text.replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    public boolean matches(String inText) {
        if (text.equals(inText)) {
            return true;
        }
        return processedText.equals(processText(inText));
    }

    public static boolean matches(String text1, String text2) {
        ApproximateMatcher matcher = new ApproximateMatcher(text1);
        return matcher.matches(text2);
    }
}