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

package com.ibm.g11n.pipeline.tools.cli;

import org.junit.Test;

/**
 * @see ListBundlesCmd
 * @author srl
 *
 */
public class ListBundlesCmdTest {

    @Test
    public void test() {
        FakebrokerMgr.run("list");
    }
    
    @Test
    public void testt() {
        String[] testFiles = {"E:\\\\notExist.json", "E:\\notJsonRoot.json", "E:\\duplicateKey.json", "E:\\test.json"};
        for(int i = 0; i < testFiles.length; i++) {
            String[] cmd = {"assess-file", "-j", "E:\\GP\\Github\\gp-java-tools\\test-gpconfig.json", "-t", "JSON", "-f", testFiles[i]};
            GPCmd.main(cmd);
        }
    }

}
