/*  
 * Copyright IBM Corp. 2015
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

import com.beust.jcommander.Parameter;

/**
 * The root class of Globalization Pipeline command.
 * 
 * @author Yoshito Umaoka
 */
abstract class BaseCmd {
    @Parameter(
            names = {"-s", "--serviceUrl"},
            description = "Globalization Pipeline service URL",
            required = true)
    protected String gpUrl;

    protected abstract void _execute();

    public void execute() {
        try {
            _execute();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
