<!--
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
-->
# Usage Example of CSV Filter with Globalization Maven Plugin

This sample maven project illustrates how you can utilize your own
custom resource filter in maven build.

## Prerequisites

- Build and install the example CSV filter.
  - `cd ../../csv-res-filter`
  - `mvn install`
- Stores credentials of your Globalization Pipeline service instance in
gpcreds.json in this folder.


## How to run the build goals

- `mvn gp:upload -Dgp.credentials.json=gpcreds.json`. This goal does following operations:
  - Create a new Globalization Pipeline bundle `CustomRes`, with default target languages.
  - Parses the contents of `src/main/resources/CustomRes.csv` and upload resource strings to the bundle as English source.
- `mvn gp:download -Dgp.credentials.json=gpcreds.json`. This goal does following operations
  - Collects translated resource strings for each target language.
  - Creates translated version of CSV file in `target/nls` folder with name `CustomRes_<langcode>.csv`. For example, the French version will be `target/nls/CustomRes_fr.csv`.
