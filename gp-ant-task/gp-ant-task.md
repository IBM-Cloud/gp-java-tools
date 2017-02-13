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

Goals available for the ant tasks are below.

| Goal | Description |
| ---- | ------------|
| gp:upload | Upload translatable resource bundle files from local file system to an instance of Globalization Pipeline service. |
| gp:download | Download translated resource bundles from an instance of Globalization Pipeline service to local file system. |


---
## <a name="TOC-Usage"></a>Usage

### <a name="TOC-Usage-AddJar"></a>Setting up Ant Task

To setup Globalization Pipeline ant task

1. Download **gp-ant-task.jar with dependencies** from maven repository. 
   You can also create the jar with dependencies from this repository with maven (using command **mvn assembly:assembly -DdescriptorId=jar-with-dependencies**)
2. Store the jar in the ANT_HOME lib folder (for automatic detection else you may need to use the command: **ant -lib {location of gp-ant-task jar} ** )
3. Copy the **build.xml** (as shown in this gp-ant-task repository folder) to your project folder

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
    $ ant gp:upload -Dgp.credentials=src/gpcreds.json
```


#### specifying credentials in build.xml

The service credentials can be also embedded in `build.xml` using `<credentials>` tag
as below.

```
    <target name="gp:upload">
        <taskdef name="upload" classname="com.ibm.g11n.pipeline.ant.GPUpload" classpath="${classpath}"/>
        <upload credentialsJson= "${gp.credentials}">
            <credentials url="${url}" userId="${userId}" password="${password}" instanceId="${instanceId}"/>
        </upload>
    </target>
```

### <a name="TOC-Usage-Basic"></a>Basic Use Case

The basic build.xml for ant tasks is shown below:
```
	<?xml version="1.0" encoding="utf-8"?>
	<project name="gp-ant-task-0.0.1-SNAPSHOT" default="none">
	    <property name="classpath" value="lib/gp-ant-task-0.0.1-SNAPSHOT-jar-with-dependencies.jar"/>
	    <property name="gp.credentials" value="src/gpcreds.json"/>
	    <property name="url" value="https://gp-rest.ng.bluemix.net/translate/rest"/>
	    <property name="userId" value="b9818b62e1db014edb42bbb8fae176b4"/>
	    <property name="password" value="Tl7edWJzDdV2DRVE5TgnFTAipQdJZ6i4"/>
	    <property name="instanceId" value="1b088966b5fed3A7fab9940496db40ac"/>
	    <property name="download.src" value="src/main/resources"/>
	    <property name="download.dest" value="target/classes"/>
	    <property name="overwrite" value="true"/>
	    <property name="languageIdStyle" value="BCP47_UNDERSCORE"/>
	    <property name="outputDir" value="target/classes"/>
	    <property name="type" value="JAVA"/>
	    <property name="sourceLanguage" value="en"/>
	    <property name="outputContentOption" value="MERGE_TO_SOURCE"/>
	    <property name="bundleLayout" value="LANGUAGE_DIR"/>
	    <property name="outputDir" value="target/classes"/>
	    <property name="includepattern" value="**/*.properties"/>
	    <property name="excludepattern" value="**/*_*.properties"/>
	    <typedef name="bundleSet" classname="com.ibm.g11n.pipeline.ant.BundleSet" classpath = "${classpath}"/>
		
		<target name="none"><echoproperties></echoproperties></target>
	    <target name="gp:upload">
	        <taskdef name="upload" classname="com.ibm.g11n.pipeline.ant.GPUpload" classpath="${classpath}"/>
	        <upload credentialsJson= "${gp.credentials}">
	            <credentials url="${url}" userId="${userId}" password="${password}" instanceId="${instanceId}"/>
	        </upload>
	    </target>
	    
	    <target name="gp:download">
	        <taskdef name="download" classname="com.ibm.g11n.pipeline.ant.GPDownload" classpath="${classpath}"/>
	        <download credentialsJson="${gp.credentials}" outputDir="${downlod.dest}">
	            <bundleSet  type="${type}" 
	                        sourceLanguage="${sourceLanguage}" 
	                        languageIdStyle="${languageIdStyle}" 
	                        outputContentOption="${outputContentOption}" 
	                        bundleLayout="${bundleLayout}"
	                        outputDir="${outputDir}">
	                <fileset dir="${download.src}" includes="${includepattern}" excludes="${excludepattern}"/>
	            </bundleSet>
	        </download>
	    </target>
	</project>
```

#### Properties in build.xml
The default values of properties required for the ant tasks are shown above.
Here we describe them


**classpath** : the location (relative to the project folder) of gp-ant-task jar with dependencies


**gp.credentials** : the file pathname which has the credentials in json form


**url** : url as specified in the credentials of globalization pipeline instance


**userId** : userId as specified in the credentials of globalization pipeline instance


**password** : password as specified in the credentials of globalization pipeline instance


**instanceId** : instanceId as specified in the credentials of globalization pipeline instance


**download.src** : the project folder path containing existing resource bundle (this property is used during download task)


**download.dest** : the project folder path where the bundles from gp instance needs to be downloaded to


**overwrite** : Specifies a boolean value to control whether **download** task overwrites translated resource bundle files already in the output directory or not. The default value "true"


**languageIdStyle** : Specifies keywords to configure the rule for composing language ID used for output resource bundle file or path name. Consult [maven plugin readme](https://github.com/IBM-Bluemix/gp-java-tools/blob/master/gp-maven-plugin.md)


**outputDir** : Specifies the output base directory for a `bundleSet`. If not specified at bundleset level, then the one specified at task level is used. This property is specific for `download` task


**type** : Specifies the resource type. Consult [maven plugin readme](https://github.com/IBM-Bluemix/gp-java-tools/blob/master/gp-maven-plugin.md)


**sourceLanguage** :Specifies BCP 47 language tag for the language used in the source bundles. The default value is "en" (English).


**outputContentOption** :Specifies keywords to control how download goal generates the contents of translated resource bundle files. Consult [maven plugin readme](https://github.com/IBM-Bluemix/gp-java-tools/blob/master/gp-maven-plugin.md)


**bundleLayout** :Specifies keywords to control output file name or path in download goal. Consult [maven plugin readme](https://github.com/IBM-Bluemix/gp-java-tools/blob/master/gp-maven-plugin.md)



**includepattern** : Default pattern to recognize files to be downloaded


**excludepattern** : Default pattern to recognize files to be excluded 




#### Uploading Source Resource Bundles (using above basic build.xml)
```
$ ant gp:upload
```
This goal does following tasks:

1. Scans files under `src/main/resources` and locates files with `.properties` extension
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
$ ant gp:download
```
This goal does following tasks:

1. Scans files under `src/main/resources` and locates Java property resource bundle
files. This is same as in the upload goal.

2. For each Java property resource bundle file, check if look up corresponding
Globalization Pipeline bundle in the instance of Globalization Pipeline service.

3. If matching Globalization Pipeline bundle is found, copies the contents of
source bundle file in local system, extracts resource strings from the Globalization
Pipeline bundle, then replaces resource string values with ones extracted
from the Globalization Pipeline bundle. The result file is generated under the
standard build output directory (`target/classes`) with language suffix, such
as `Messages_fr.properties` for French. This operation is done for all available
target languages in the Globalization Pipeline bundle.


## <a name="TOC-Usage-Advanced"></a>Advanced Use Cases

The bundleset in build.xml can be modified to support extra features for `download` task. Example
```
	<download credentialsJson="${gp.credentials}" outputDir="${downlod.dest}">
            <bundleSet  type="${type}" 
                        sourceLanguage="${sourceLanguage}" 
                        languageIdStyle="${languageIdStyle}" 
                        outputContentOption="${outputContentOption}" 
                        bundleLayout="${bundleLayout}"
                        outputDir="${outputDir}">
                <fileset dir="${download.src}" includes="${includepattern}" excludes="${excludepattern}"/>
		<targetLanguage lang="en"/>
		<targetLanguage lang="ja"/>
		<languageMap from="pt-BR" to="pt"/>
            </bundleSet>
	    <bundleSet  type="JSON" 
                        sourceLanguage="ja" 
                        languageIdStyle="BCP47" 
                        outputContentOption="TRANSLATED_ONLY" 
                        bundleLayout="LANGUAGE_DIR">
                <fileset dir="src/main/xyz" includes="**/*.properties" excludes="**/*_*.properties/>
		<targetLanguage lang="en"/>
		<targetLanguage lang="ja"/>
		<languageMap from="pt-BR" to="pt"/>
            </bundleSet>
	    .
	    .
        </download>
```

In the above snippet, the following tags are introduced within bundleSet

**targetLanguage** : Specifies a list of translation target languages by BCP 47 language tags. For the above usecase, only translation files in the target languages are download for that bundle

**languageMap** : Specifies custom language mappings. Each nested element name is a BCP 47 language tag used by Globalization Pipeline service instance, and the element value is a language ID used for output resource file or path name. For example, Globalization Pipeline service uses language ID "pt-BR" for Brazilian Portuguese. If you want to use just "pt" for output file or path name, you can specify the mapping as `<languageMap from="pt-BR" to="pt"/>`


Also that the user can define multiple bundleSet tags within the download task with overridden values for the attributes associated with the bundleset



