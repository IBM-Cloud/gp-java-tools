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
  * [Setting Up Globalization Pipeline Ant Task](#TOC-Usage-Setup)
  * [Tasks](#TOC-Usage-Tasks)
  * [Specifying Globalization Pipeline Service Credentials](#TOC-Usage-Credentials)
  * [Basic Use Case](#TOC-Usage-Basic)
  * [Task Parameters in build.xml](#TOC-Parameters)
  * [Example](#TOC-Usage-Example)

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
in your Ant build.xml as below.

```xml
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

```xml
    <target name="upload-resources">
        <gp:upload credentialsJson= "gpcreds.json" sourceDir="src/main/resources"/>
    </target>
```

#### Specifying credentials in build.xml

The service credentials can be also embedded in `build.xml` using `<credentials>` tag
as below.

```xml
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

**build.xml** below illustrates basic use cases of the ant tasks:
```xml
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
  
  <!-- Downloads translated .properties files from the Globalization Pipeline services intances to outputDir -->
  <target name="download-translations">
    <gp:download credentialsJson="${gp.credentials}" sourceDir="src/main/resources" outputDir="target/classes" />
  </target>
</project>
```

The ant target `upload-resources` in the example above does following operations:

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

2. For each Java property resource bundle file, looks up corresponding
Globalization Pipeline bundle in the instance of Globalization Pipeline service.

3. If matching Globalization Pipeline bundle is found, copies the contents of
source bundle file in local system, extracts resource strings from the Globalization
Pipeline bundle, then replaces resource string values with ones extracted
from the Globalization Pipeline bundle. The result file is generated under the
standard build output directory (`target/classes`) (*outputDir* attribute)  with language suffix, such
as `Messages_fr.properties` for French. This operation is done for all available
target languages in the Globalization Pipeline bundle.

### <a name="TOC-Parameters"></a>Task Parameters

### upload task
The upload task should be configured using `gp:upload` task definition. The attributes and nested elements
of `gp:upload` are described below:

|Attribute|Description|Required|
| ------- | --------- | ------ |
|credentialsJson| Specifies the pathname of Globalization Pipeline Instance credentials file|Yes (if the nested element `credentials` is not used)|
|sourceDir| Specifies the pathname for the location where all the relevant bundle files to be uploaded are kept|Yes|
##### Nested elements for upload task
##### credentials (optional, can be omitted if credentialsJson is specified.)
|Attribute|Description|Required|
| ------- | --------- | ------ |
|url| Specifies the instance url | Yes |
|userId| Specifies the userId | Yes |
|password| Specifies the password | Yes |
|instanceId| Specifies the instance id | Yes |

##### targetLanguage (optional)
|Attribute|Description|Required|
| ------- | --------- | ------ |
|lang     | Specifies the MT language which the uploaded file should be translated to| Yes |

##### bundleSet (optional)
|Attribute|Description|Required|
| ------- | --------- | ------ |
|  type    | Type of resource bundle file (JSON, JAVA (properties), AMDJS, ...)| No, defaults to JAVA properties file format|
|sourceLanguage|Specifies BCP 47 language tag for the language used in the source bundles|No, The default language of `en` is used|
|languageIdStyle| Specifies one of following keywords to configure the rule for composing language ID used for output resource bundle file or path name.<ul><li><b>BCP47_UNDERSCORE</b> BCP 47 language tag, replacing '-' with '_'. For example, zh_Hant for Traditional Chinese.</li><li><b>BCP47</b> BCP 47 language tag itself. For example, zh-Hant for Traditional Chinese </li></ul>| No, The default value is BCP47_UNDERSCORE|

##### targetLanguage (nested within bundleSet, optional too)
|Attribute|Description|Required|
| ------- | --------- | ------ |
|lang     | Specifies the MT language which the uploaded file should be translated to| Yes |

##### fileset (nested within bundleSet, required if bundleSet is used)
|Attribute|Description|Required|
| ------- | --------- | ------ |
|dir| Specifies the source directory location for the bundleSet to be used as reference | Yes |
|includes| Specifies the file pattern that needs to be included for uploading|No, but recommended|
|excludes| Specifies the file pattern that needs to be excluded for uploading|No|

### download task
The download task should be configured using `gp:download` task definition. The attributes and nested elements
of `gp:download` are described below:

|Attribute|Description|Required|
| ------- | --------- | ------ |
|credentialsJson| Specifies the pathname of Globalization Pipeline Instance credentials file|Yes (if the nested element `credentials` is not used)|
|sourceDir| Specifies the pathname for the location where all the relevant bundle files to be referenced for download are kept|Yes|
|outputDir| Specifies the pathname for the location where all the resource bundle files should be downloaded|No, defaults to `target/classes` directory|
##### Nested elements for download task
##### credentials (optional, can be omitted if credentialsJson is specified.)
|Attribute|Description|Required|
| ------- | --------- | ------ |
|url| Specifies the instance url | Yes |
|userId| Specifies the userId | Yes |
|password| Specifies the password | Yes |
|instanceId| Specifies the instance id | Yes |

##### targetLanguage (optional)
|Attribute|Description|Required|
| ------- | --------- | ------ |
|lang     | Specifies the MT language which the uploaded file should be translated to| Yes |

##### bundleSet (optional)
|Attribute|Description|Required|
| ------- | --------- | ------ |
|  type    | Type of resource bundle file (JSON, JAVA (properties), AMDJS, ...)| No, defaults to JAVA properties file format|
|sourceLanguage|Specifies BCP 47 language tag for the language used in the source bundles|No, The default language of `en` is used|
|languageIdStyle| Specifies one of following keywords to configure the rule for composing language ID used for output resource bundle file or path name.<ul><li><b>BCP47_UNDERSCORE</b> BCP 47 language tag, replacing '-' with '_'. For example, zh_Hant for Traditional Chinese.</li><li><b>BCP47</b> BCP 47 language tag itself. For example, zh-Hant for Traditional Chinese </li></ul>| No, The default value is BCP47_UNDERSCORE|
|outputDir|Specifies the output base directory for this bundleSet|No, If not specified, `outputDir` specified at `gp:download` level is used|
|outputContentOption|Specifies one of following keywords to control how download goal generates the contents of translated resource bundle files.<ul><li><b>MERGE_TO_SOURCE</b> Duplicates the contents of the source bundle and replaces only translated resource strings. This option might not be implemented by some format types. In this case, TRANSLATED_WITH_FALLBACK is used instead.</li><li><b>TRANSLATED_WITH_FALLBACK</b> Emits only resource strings (with a simple header if applicable). When translated string value is not available, the value in the source language is used.</li><li><b>TRANSLATED_ONLY</b> Emits only resource strings (with a simple header if applicable). When translated string value is not available, do not include the key in the output.</li><li><b>MERGE_REVIEWED_TO_SOURCE</b> Duplicate the contents of the source bundle and replaces only translated resource strings marked as reviewed. This option might not be implemented by some format types. In this case, REVIEWED_WITH_FALLBACK is used instead.</li><li><b>REVIEWED_WITH_FALLBACK</b> Emits only resource strings marked as reviewed. When translated string value is not available, or not marked as reviewed, the value in the source language is used.</li><li><b>REVIEWED_ONLY</b> Emits only resource strings marked as reviewed. When translated string value is not available, or translated not marked as reviewed, do not include the key in the output.|No, The default value is MERGE_TO_SOURCE.|
|bundleLayout|Specifies one of following keywords to control output file name or path in download goal.<ul><li><b>LANGUAGE_SUFFIX</b> In the same directory with the source bundle file, with extra language suffix. For example, if the source bundle file is com/ibm/g11n/MyMessages.properties, then the French version will be com/ibm/g11n/MyMessages_fr.properties.</li><li><b>LANGUAGE_SUBDIR</b> In a language sub-directory under the directory where the source bundle file is placed. For example, if the source bundle file is res/MyMessages.json, then the French version will be res/fr/MyMessages.json.</li><li><b>LANGUAGE_DIR</b> In a language directory at the same level with the source bundle file. For example, if the source bundle file is com/ibm/g11n/en/MyMessages.properties, then the French version will be com/ibm/g11n/fr/MyMessages.properties.</li>|No, The default value is LANGUAGE_SUFFIX.|

##### targetLanguage (nested within bundleSet, optional too)
|Attribute|Description|Required|
| ------- | --------- | ------ |
|lang     | Specifies the MT language which the uploaded file should be translated to| Yes |

##### languageMap (nested within bundleSet, optional)
|Attribute|Description|Required|
| ------- | --------- | ------ |
|from     | Specifies the language code used in bundle files/directories which needs to be downloaded | Yes|
|to       | Specifies the language code to be used when naming the downloaded file/directory|Yes|

##### fileset (nested within bundleSet, required if bundleSet is used)
|Attribute|Description|Required|
| ------- | --------- | ------ |
|dir| Specifies the source directory location for the bundleSet to be used as reference | Yes |
|includes| Specifies the file pattern that needs to be included for reference when downloading|No, but recommended|
|excludes| Specifies the file pattern that needs to be excluded for reference|No|

### <a name="TOC-Usage-Example"></a>Example

`example` directory contains some usage examples. Please read the
[instruction](example/README.md).
