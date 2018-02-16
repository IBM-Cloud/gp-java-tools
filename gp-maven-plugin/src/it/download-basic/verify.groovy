/*  
 * Copyright IBM Corp. 2017, 2018
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

import java.io.*
import com.ibm.g11n.pipeline.maven.*;

try {
    println "verifying that download happened successfully"
    String location = bldDir + "/it/download-basic"
    String targetLocation = location + "/target/classes/com/bundle1"
    String targetLocation2 = location + "/target/classes/com/bundle2"
    def dir = new File(targetLocation)
    int filecount = 0
    int keycount = 5 // including opening and closing braces which take 1 line each
    dir.traverse { file ->
        if (!file.directory) {
            if (file.name.endsWith("json")) {
                File jsonfile = new File(targetLocation + "/" + file.name)
                def lines = jsonfile.readLines()
                println file.name + lines.size()
                assert lines.size() == keycount
                filecount++
            }
        }
    }
    assert filecount == 20

    def dir2 = new File(targetLocation2)
    dir2.traverse { file ->
        if (!file.directory) {
            assert !file.name.contains("_en_")
        }
    }
} catch( Throwable t ) {
    t.printStackTrace()
    return false
}
