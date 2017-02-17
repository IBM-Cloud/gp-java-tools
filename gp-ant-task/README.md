# Globalization Pipeline Ant Task User Guide
<!--
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
-->
* [Overview](#TOC-Overview)
* [Prerequisites](#TOC-Prerequisites)
* [Usage](#TOC-Usage)
  * [Seting Up Globalization Pipeline Ant Task](#TOC-Usage-Setup)
  * [Tasks](#TOC-Usage-Tasks)
  * [Specifying Globalization Pipeline Service Credentials](#TOC-Usage-Credentials)
  * [Basic Use Case](#TOC-Usage-Basic)
  * [Example](#Example)

---
## <a name="TOC-Overview"></a>Overview

Globalizaton Pipeline Ant Task is an antlib designed for integrating Globalization
Pipeline service with Apache Ant. The antlib includes custom tasks for uploading
translatable resource bundles from local file system to an instance of Globalization
Pipeline service and downloading translated resource bundles to local file system.

---
## <a name="TOC-Prerequisites"></a>Prerequisites

This antlib runs on Java Runtime Environment 7 or later versions. The minimum ant
version is 1.9.

---
## <a name="TOC-Usage"></a>Usage

### <a name="TOC-Usage-Setup"></a>Seting Up Globalization Pipeline Ant Task

To setup Globalization Pipeline Ant Task

1. Download the Globalization Pipeline Ant Task jar file (gp-ant-task-X.X.X-with-dependencies.jar)
from the official [release page](https://github.com/IBM-Bluemix/gp-java-tools/releases).
2. Set up name space for the Globalization Pipleline tasks and task definition
as below in your Ant build.xml as below.

```
<?xml version="1.0" encoding="utf-8" ?>
<project name="My Project" xmlns:gp="antlib:com.ibm.g11n.pipeline.ant">

<taskdef uri="antlib:com.ibm.g11n.pipeline.ant" resource="com/ibm/g11n/pipeline/ant/antlib.xml">
  <classpath path="${path-to-gp-jar}/gp-ant-task-X.X.X-with-dependencies.jar"/>
</taskdef>
```
Note: You might place Globalization Pipeline Ant Task jar file in your Ant installations
library directory (**$ANT_HOME/lib**). In this case, Ant will automatically find the task
definitions without `<taskdef>` declaration.

### <a name="TOC-Usage-Tasks"></a>Tasks

Following Ant tasks are currently available.

| Task | Description |
| ---- | ------------|
| upload | Upload translatable resource bundle files from local file system to an instance of Globalization Pipeline service. |
| download | Download translated resource bundles from an instance of Globalization Pipeline service to local file system. |


### <a name="TOC-Usage-Credentials"></a>Specifying Globalization Pipeline Service Credentials

The ant task requires *service instance administrator* credentials for a Globalization
Pipeline service instance. Please refer [Quick Start Guide](https://github.com/IBM-Bluemix/gp-common#quick-start-guide)
to see how to get service credentials information of your Globalization Pipeline instance.

There are two ways to specify the service credentials.

#### JSON file

The ant task can read service credentials from a JSON file. The JSON file must
contain "url", "userId", "password" and "instanceId" fields as below.

```
{
  "url": "https://gp-rest.ng.bluemix.net/translate/rest",
  "userId": "f02d4de9f115cc8b6e8c75a6c995f075",
  "password": "YOfm4qhW8D4stazDL7cBOBB1YV+kf1qa",
  "instanceId": "d38f803b34a3aa36e39174bcf2d5f941"
}
```
Note: This is an example. You must replace field values with actual values used by
your Globalization Pipeline service instance.

In your Ant build.xml, specify the JSON file in **credentialsJson** attribute in
each Globalization Pipeline Ant task as below:
```
    <target name="upload-resources">
        <gp:upload credentialsJson= "gpcreds.json" sourceDir="src/main/resources"/>
    </target>
```

#### Specifying credentials in build.xml

The service credentials can be also embedded in `build.xml` using `<credentials>` tag
as below.

```
  <target name="upload-resources">
    <gp:upload sourceDir="src/main/resources">
      <credentials url="https://gp-rest.ng.bluemix.net/translate/rest"
                userId="f02d4de9f115cc8b6e8c75a6c995f075"
                password="YOfm4qhW8D4stazDL7cBOBB1YV+kf1qa"
                instanceId="d38f803b34a3aa36e39174bcf2d5f941"/>
    </gp:upload>
  </target>
```
Note: This is an example. You must replace attribute values with actual values
used by Globalization Pipeline service instance.

### <a name="TOC-Usage-Basic"></a>Basic Use Case

**build.xml** below illustrates basic use cases of the tant tasks:
```
<?xml version="1.0" encoding="utf-8"?>
<project name="Example Project" xmlns:gp="antlib:com.ibm.g11n.pipeline.ant">

  <taskdef  uri="antlib:com.ibm.g11n.pipeline.ant" resource="com/ibm/g11n/pipeline/ant/antlib.xml">
    <classpath path="my-antlib-dir/gp-ant-task-X.X.X-with-dependencies.jar"/>
  </taskdef>

  <!--Specify the location of the credentials json file -->
  <property name="gp.credentials" value="creds.json"/>

  <!-- Uploads .properties files in sourceDir to the Globalization Pipelne service instance -->
    <target name="upload-resources">
      <gp:upload credentialsJson= "${gp.credentials}" sourceDir="src/main/resources" />
  </target>

  <!-- Downloads translated .properties files from the Globalization Pipeline services intances to outputDir ->
  <target name="download-translations">
    <gp:download credentialsJson="${gp.credentials}" sourceDir="src/main/resources" outputDir="target/classes" />
  </target>
</project>
```

The ant target `upload-resources` in the example ablve does following operations:

1. Scans files under `src/main/resources` (*sourceDir* attribute) and locates files with `.properties` extension
(but excluding files with '_'(underscore) character in its file name, such as
`Messages_fr.properties`).

2. For each Java property resource bundle file, checks if corresponding Globalization
Pipeline bundle already exists or not. If it's not available, creates a new bundle
with all translation target languages currently configured in Machine Translation. English is used as the
translation source language.

3. Extracts resource strings from each file, and uploads them to the matching
Globalization Pipeline bundle as the translation source.

This task will synchronize the contents of a Globalization Pipeline bundle with
the contents of local file. When a resource key was deleted from local file, it will
be also deleted from the corresponding Globalization Pipeline bundle. If a resource
string value was changed, this operation refreshes the corresponding resource string's
value, which eventually triggers re-translation. When the contents is not changed
since last invocation, this operation does not make any changes in the Globalization
Pipeline bundles.

So the best practice would be invoking the task when any changes were made in
translatable resource bundle files, although it's not harmful to invoke the goal
at any time.

This ant target `download-translations` above does following operations:

1. Scans files under `src/main/resources`(*sourceDir* attribute) and locates Java property resource bundle
files. This is same as in the upload goal.

2. For each Java property resource bundle file, check if look up corresponding
Globalization Pipeline bundle in the instance of Globalization Pipeline service.

3. If matching Globalization Pipeline bundle is found, copies the contents of
source bundle file in local system, extracts resource strings from the Globalization
Pipeline bundle, then replaces resource string values with ones extracted
from the Globalization Pipeline bundle. The result file is generated under the
standard build output directory (`target/classes`) (*outputDir* attribute)  with language suffix, such
as `Messages_fr.properties` for French. This operation is done for all available
target languages in the Globalization Pipeline bundle.


### <a name="TOC-Usage-Example"></a>Example

`example` directory contains some usage examples. Please read the
[instruction](example/README.md).
