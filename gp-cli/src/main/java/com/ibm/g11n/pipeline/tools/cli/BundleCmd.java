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

import com.beust.jcommander.Parameter;

/**
 * A base class of commands taking bundle ID as an input.
 * 
 * @author Yoshito Umaoka
 */
abstract class BundleCmd extends BaseCmd {
    @Parameter(
            names = { "-b", "--bundle"},
            description = "Bundle ID",
            required = true)
    protected String bundleId;
}
