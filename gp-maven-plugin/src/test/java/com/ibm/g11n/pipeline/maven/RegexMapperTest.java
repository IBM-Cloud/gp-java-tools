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

import org.junit.Assert;
import org.junit.Test;


/**
 * RegexMapper test cases
 * 
 * @author "Yoshito Umaoka"
 */
public class RegexMapperTest {

    @Test
    public void testMap() {
        final String[][] TESTS = {
                {"com/ibm/g11n/Test.java", "(.+)\\.java", "$1", "com/ibm/g11n/Test"},
                {"com/ibm/g11n/Test", "/", "\\.", "com.ibm.g11n.Test"},
                {"abc-123", "def", "xyz", "abc-123"},   // no matches
        };

        for (String[] test : TESTS) {
            RegexMapper mapper = new RegexMapper(test[1], test[2]);
            String result = mapper.map(test[0]);
            Assert.assertEquals(result, test[3]);
        }
    }
}
