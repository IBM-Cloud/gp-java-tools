# Globalization Pipeline Ant task User Guide
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
* [Goals](#TOC-Goals)
* [Usage](#TOC-Usage)
  * [Setup](#TOC-Usage-AddJar)
  * [Specifying Globalization Pipeline Service Credentials](#TOC-Usage-Credentials)
  * [Basic Use Case](#TOC-Usage-Basic)
  * [Advanced Use Cases](#TOC-Usage-Advanced)
  * [Example](#Example)


---
## <a name="TOC-Overview"></a>Overview

Globalizaton Pipeline Ant Task is designed for integrating Globalization
Pipeline service with ant tasks. The tasks can upload translatable resource
bundles from local file system to an instance of Globalization Pipeline service
and download translated resource bundles to local file system.

---
## <a name="TOC-Prerequisites"></a>Prerequisites

This task runs on Java Runtime Environment 7 or later versions. The minimum
Apache Maven version is 3.0. The minimum ant version is 1.9

---
## <a name="TOC-Goals"></a>Goals

basic targets available for the ant tasks are below.

| Goal | Description |
| ---- | ------------|
| upload-resources | Upload translatable resource bundle files from local file system to an instance of Globalization Pipeline service. |
| download-translations | Download translated resource bundles from an instance of Globalization Pipeline service to local file system. |


---
## <a name="TOC-Usage"></a>Usage

### <a name="TOC-Usage-AddJar"></a>Setting up Ant Task

To setup Globalization Pipeline ant task

1. Download the **gp-ant-task jar with dependencies**. 
   You can also create the jar with dependencies from this repository with maven (using command **mvn install**)
2. Store the jar in the ANT_HOME lib folder (for automatic detection else you may need to use the command: **ant -lib {location of gp-ant-task jar} ** 
											 or you may mention the classpath for the jar file in build.xml)
3. Copy the **build.xml** (as shown in this gp-ant-task repository example folder) to your project folder

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
To use the credentials stored in a JSON file, specify a system property `gp.credentialsJson`.
For example, if the file pathname is `src/gpcreds.json`, add `-Dgp.credentialsJson=src/gpcreds.json`
to ant command line.
example: 
```
    $ ant upload-resources -Dgp.credentials=src/gpcreds.json
```


#### specifying credentials in build.xml

The service credentials can be also embedded in `build.xml` using `<credentials>` tag
as below.

```
    <target name="upload-resources">
        <gp:upload credentialsJson= "${gp.credentials}" sourceDir="src/main/resources">
        	<credentials url="${gp.url}" userId="${gp.userId}" password="${gp.password}" instanceId="${gp.instanceId}"/>
        </gp:upload>
    </target>
```

### <a name="TOC-Usage-Basic"></a>Basic Use Case

The basic build.xml for ant tasks is shown below:
```
	<?xml version="1.0" encoding="utf-8"?>
<project name="gp-ant-task-1.1.5-SNAPSHOT" default="download-translations" 
	xmlns:gp="antlib:com.ibm.g11n.pipeline.ant">
	
	<taskdef  uri="antlib:com.ibm.g11n.pipeline.ant" resource="com/ibm/g11n/pipeline/ant/antlib.xml">
		<!-- If the gp-ant-task jar is not added to ANT_HOME/lib folder, 
			then you may include the classpath for the jar like in the comment below-->
		<!--<classpath path="lib/gp-ant-task-1.1.5-SNAPSHOT-with-dependencies.jar"/>-->
	</taskdef>
	
	<!--Specify the location of the credentials json file-->
	<property name="gp.credentials" value="src/credentials.json"/>
	
	<!-- basic usecase for uploading resources(this uploads java .properties files by default)-->
    <target name="upload-resources">
        <gp:upload credentialsJson= "${gp.credentials}" sourceDir="src/main/resources">
        </gp:upload>
    </target>
	
    <!-- basic use case for downloading resources (this downloads the java .properties files to the target/classes folder of the project) -->
    <target name="download-translations">
        <gp:download credentialsJson="${gp.credentials}" sourceDir="src/main/resources" outputDir="target/classes">
        </gp:download>
    </target>

```


#### Uploading Source Resource Bundles (using above basic build.xml)
```
$ ant upload-resources
```
This target does following tasks:

1. Scans files under `src/main/resources` (*sourceDir* attribute) and locates files with `.properties` extension
(but excluding files with '_'(underscore) character in its file name, such as
`Messages_fr.properties`).

2. For each Java property resource bundle file, checks if corresponding Globalization
Pipeline bundle already exists or not. If it's not available, creates a new bundle
with all translation target languages currently configured in Machine Translation. English is used as the
translation source language.

3. Extracts resource strings from each file, and uploads them to the matching
Globalization Pipeline bundle as the translation source.

This operation will synchronize the contents of a Globalization Pipeline bundle with
the contents of local file. When a resource key was deleted from local file, it will
be also deleted from the corresponding Globalization Pipeline bundle. If a resource
string value was changed, this operation refreshes the corresponding resource string's
value, which eventually triggers re-translation. When the contents is not changed
since last invocation, this operation does not make any changes in the Globalization
Pipeline bundles.

So the best practice would be invoking this goal when any changes were made in
translatable resource bundle files, although it's not harmful to invoke the goal
at any time.

#### Downloading Translated Resource Bundles (using above basic build.xml)
```
$ ant download-translations
```
This target does following tasks:

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


#### Properties/Attributes in build.xml
The definitions and default values of properties/attributes for the ant targets are described below.
These properties/attributes can be seen in example/build.xml:


**gp.credentials** : the file pathname which has the credentials in json form


**gp.url** : url as specified in the credentials of globalization pipeline instance


**gp.userId** : userId as specified in the credentials of globalization pipeline instance


**gp.password** : password as specified in the credentials of globalization pipeline instance


**gp.instanceId** : instanceId as specified in the credentials of globalization pipeline instance


**outputDir** : Specifies the output base directory for a `bundleSet`. If not specified at bundleset level, then the one specified at task level is used. This property is specific for `download` task. This is mandatory to be defined at task level if not defined at bundleset level


**sourceDir** : Specifies the default directory to be scanned when uploading/downloading resource files. This is mandatory for targets involving uploads and is also mandatory for targets involving downloads if the source directory(download.src property) is not defined at bundleset level 


**gp.download.src** : the project folder path containing existing resource bundle (this property is used during download task)


**gp.download.dest** : the project folder path where the bundles from gp instance needs to be downloaded to


**gp.overwrite** : Specifies a boolean value to control whether **download** task overwrites translated resource bundle files already in the output directory or not. The default value "true"


**gp.languageIdStyle** : Specifies keywords to configure the rule for composing language ID used for output resource bundle file or path name. Consult [maven plugin readme](https://github.com/IBM-Bluemix/gp-java-tools/blob/master/gp-maven-plugin.md)


**gp.type** : Specifies the resource type. Consult [maven plugin readme](https://github.com/IBM-Bluemix/gp-java-tools/blob/master/gp-maven-plugin.md)


**gp.sourceLanguage** :Specifies BCP 47 language tag for the language used in the source bundles. The default value is "en" (English).


**gp.outputContentOption** :Specifies keywords to control how download goal generates the contents of translated resource bundle files. Consult [maven plugin readme](https://github.com/IBM-Bluemix/gp-java-tools/blob/master/gp-maven-plugin.md)


**gp.bundleLayout** :Specifies keywords to control output file name or path in download goal. Consult [maven plugin readme](https://github.com/IBM-Bluemix/gp-java-tools/blob/master/gp-maven-plugin.md)



**gp.includepattern** : Default pattern to recognize files to be downloaded


**gp.excludepattern** : Default pattern to recognize files to be excluded 


## <a name="TOC-Usage-Advanced"></a>Advanced Use Cases

The bundleset in build.xml can be modified to support extra features for `download` task. Example
```
	<!-- advanced use case for downloading resources. This downloads java properties files and json properties files and organizes them differently -->
	<target name="download-translations-adv">
		<property name="gp.url" value="https://gp-rest.ng.bluemix.net/translate/rest"/>
		<property name="gp.userId" value="b9818b62e1db014dedb2bbb8eaf176b4"/>
		<property name="gp.password" value="Tl7edjvzdwd2Rqjpztdbitagfendv6i4"/>
		<property name="gp.instanceId" value="1b088966b5fed337baf9940496db40ac"/>
		<property name="gp.download.src" value="resources"/>
		<property name="gp.download.dest" value="target"/>
		<property name="gp.overwrite" value="true"/>
		<property name="gp.languageIdStyle" value="BCP47_UNDERSCORE"/>
		<property name="gp.outputDir" value="target"/>
		<property name="gp.type" value="JSON"/>
		<property name="gp.sourceLanguage" value="en"/>
		<property name="gp.outputContentOption" value="MERGE_TO_SOURCE"/>
		<property name="gp.bundleLayout" value="LANGUAGE_DIR"/>
		<property name="gp.includepattern" value="**/*.json"/>
		<property name="gp.excludepattern" value="**/*_*.json"/>
		<gp:download>
			<credentials url="${gp.url}" userId="${gp.userId}" password="${gp.password}" instanceId="${gp.instanceId}"/>
			<targetLanguage lang="es"/>
			<targetLanguage lang="pt-BR"/>
			<bundleSet
				type="${gp.type}" 
				sourceLanguage="${gp.sourceLanguage}" 
				languageIdStyle="${gp.languageIdStyle}" 
				outputContentOption="${gp.outputContentOption}" 
				bundleLayout="${gp.bundleLayout}"
				outputDir="${gp.download.dest}">
				<targetLanguage lang="ja"/>
				<targetLanguage lang="fr"/>
				<targetLanguage lang="ko"/>
				<targetLanguage lang="pt-BR"/>
				<languageMap from="pt-BR" to="pt"/>
				<fileset dir="${gp.download.src}" includes="${gp.includepattern}" excludes="${gp.excludepattern}"/>
			</bundleSet>
			<bundleSet
				type="JAVA" 
				sourceLanguage="en" 
				languageIdStyle="BCP47" 
				outputDir="${gp.download.dest}">
				<fileset dir="${gp.download.src}" includes="**/*.properties"/>
			</bundleSet>
		</gp:download>
	</target>
```

In the above snippet, the following tags are introduced within bundleSet

**targetLanguage** : Specifies a list of translation target languages by BCP 47 language tags. For the above usecase, only translation files in the target languages are download for that bundle

**languageMap** : Specifies custom language mappings. Each nested element name is a BCP 47 language tag used by Globalization Pipeline service instance, and the element value is a language ID used for output resource file or path name. For example, Globalization Pipeline service uses language ID "pt-BR" for Brazilian Portuguese. If you want to use just "pt" for output file or path name, you can specify the mapping as `<languageMap from="pt-BR" to="pt"/>`


Also that the user can define multiple bundleSet tags within the download task with overridden values for the attributes associated with the bundleset

## <a name="Example"></a>Example
To demo the functionality of ant tasks, please follow the steps:
1. In the *example* directory of this repository, open file credentials.json and edit the userId, password, instanceId. These credentials should be of a 
   valid globalization pipeline instance when no bundlesets present.
2. Make sure that latest gp-ant-task.jar is present with all dependencies in the example folder.
3. Verify that the targets upload-resources, download-translations, upload-resources-adv, and download-translations-adv are defined in the example/build.xml file.
4. Run the target ** ant upload-resources ** with the base directory of *example*
5. When the **upload-resources** target is run, then the JAVA .properties files nested under `resources` folder (under `com` directory) are uploaded to the 
   globalization instance in the default target languages as configured for Machine Translation
6. Now, run the target ** ant download-translations **. This command should download the bundle com.test and place the .properties files in target/com folder 
   with translated files in the format like test_es.properties, test_ja.properties ....
7. Now, run the target ** ant upload-resources-adv **. This will upload the source json file (resources/com/test.json) to the globalization pipeline instance. 
   The desired translations are specified using the targetLanguage tags. 
8. Now run the target ** ant download-resources-adv **. When the target is triggered using ant then 
	1. The translated json files are downloaded from the globalization pipeline instance in the target languages of japanese, french, korean, and portuguese (provided they are present in those languages in the instance. Target languages for which the json doesn't exist are not downloaded).The translated files are organized into directories (based on the target language) because of **LANGUAGE_DIR** setting for bundlelayout
	 For example, if the instance contains json files in all target languages, then after download, following files are created
	 target/fr/test.json, target/ko/test.json, target/pt/test.json (because pt-BR should be renamed to pt as per **languageMap** property), target/ja/test.json
	 2. The translated java properties files are downloaded in the targetlanguages of es, and pt-BR (specified at the parent level) as no target languages are specified at bundleset level. So if the com/test.properties is present in the instance (along with the versions in the target languages), then following files are downloaded target/test_es.properties and target/test_pt_BR.properties.
	 
	 
The developer can create his/her own targets using the above examples as references.
	   



