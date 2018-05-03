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
package com.ibm.g11n.pipeline.maven;

import java.util.regex.Pattern;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * RegexMapper is used for specifying mapping rules using
 * regular expression.
 * 
 * @author "Yoshito Umaoka"
 */
public class RegexMapper {
    @Parameter(required = true)
    private String pattern;

    @Parameter(required = true)
    private String replacement;

    public RegexMapper() {
    }

    public RegexMapper(String pattern, String replacement) {
        this.pattern = pattern;
        this.replacement = replacement;
    }

    /**
     * @return the pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * @param pattern the pattern to set
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    /**
     * @return the replacement
     */
    public String getReplacement() {
        return replacement;
    }

    /**
     * @param replacement the replacement to set
     */
    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }


    public String map(String s) {
        assert pattern != null;
        assert replacement != null;

        return Pattern.compile(pattern).matcher(s).replaceAll(replacement);
    }
}
