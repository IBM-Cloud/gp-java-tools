# Globalization Pipeline Maven Plugin User Guide
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
  * [Adding Globalization Pipeline Plugin](#TOC-Usage-AddPlugin)
  * [Specifying Globalization Pipeline Service Credentials](#TOC-Usage-Credentials)
  * [Basic Use Case](#TOC-Usage-Basic)
  * [Advanced Use Cases](#TOC-Usage-Advanced)
* [Configuration Parameter Reference](#TOC-ConfigParamRef)


---
## <a name="TOC-Overview"></a>Overview

Globalizaton Pipeline Maven Plugin is designed for integrating Globalization
Pipeline service with Maven build. The plugin can upload translatable resource
bundles from local file system to an instance of Globalization Pipeline service
and download translated resource bundles to local file system.

---
## <a name="TOC-Prerequisites"></a>Prerequisites

This plugin runs on Java Runtime Environment 7 or later versions. The minimum
Apache Maven version is 3.0.

---
## <a name="TOC-Goals"></a>Goals

Goals available for this plugin are below.

| Goal | Description |
| ---- | ------------|
| gp:upload | Upload translatable resource bundle files from local file system to an instance of Globalization Pipeline service. |
| gp:download | Download translated resource bundles from an instance of Globalization Pipeline service to local file system. |

---
## <a name="TOC-Usage"></a>Usage

### <a name="TOC-Usage-AddPlugin"></a>Adding Globalization Pipeline plugin

To integrate Globalization Pipeline plugin with a Maven build, add <plugin> section
in pom.xml.

```
<project>
    [...]
    <build>
        [...]
        <plugins>
            <plugin>
                <groupId>com.ibm.g11n.pipeline</groupId>
                <artifactId>gp-maven-plugin</artifactId>
                <version>1.1.4</version>
            </plugin>
            [...]
        </plugins>
    </build>
    [...]
</project>
```

### <a name="TOC-Usage-Credentials"></a>Specifying Globalization Pipeline Service Credentials

The plugin requires *service instance administrator* credentials for a Globalization
Pipeline service instance. Please refer [Quick Start Guide](https://github.com/IBM-Bluemix/gp-common#quick-start-guide)
to see how to get service credentials information of your Globalization Pipeline instance.

There are two ways to specify the service credentials.

#### JSON file

The plugin can read service credentials from a JSON file. The JSON file must
contain "url", "userId", "password" and "instanceId" fields as below.

```
{
  "url": "https://gp-rest.ng.bluemix.net/translate/rest",
  "userId": "f02d4de9f115cc8b6e8c75a6c995f075",
  "password": "YOfm4qhW8D4stazDL7cBOBB1YV+kf1qa",
  "instanceId": "d38f803b34a3aa36e39174bcf2d5f941"
}
```
To use the credentials stored in a JSON file, specify a system property `gp.credentials.json`.
For example, if the file name is `gpcreds.json`, add `-Dgp.credentials.json=gpcreds.json`
to Maven command line.


#### Plugin configuration in pom.xml

The service credentials can be also embedded in pom.xml using `<credentials>` configuration
as below.

```
            <plugin>
                <groupId>com.ibm.g11n.pipeline</groupId>
                <artifactId>gp-maven-plugin</artifactId>
                <version>1.1.4</version>
                <configuration>
                    <credentials>
                        <url>https://gp-rest.ng.bluemix.net/translate/rest</url>
                        <userId>f02d4de9f115cc8b6e8c75a6c995f075</userId>
                        <password>YOfm4qhW8D4stazDL7cBOBB1YV+kf1qa</password>
                        <instanceId>d38f803b34a3aa36e39174bcf2d5f941</instanceId>
                    </credentials>
                </configuration>
            </plugin>
```

### <a name="TOC-Usage-Basic"></a>Basic Use Case

By default, Globalization Pipeline plugin assumes all translatable English resource bundle
files are in Java property resource bundle format (*.properties) and they are placed under
the standard directory (src/main/resources). If your project follow this convention,
you don't need any extra configurations other than adding the plugin and specifying
Globalization Pipeline service credentials.

#### Uploading Source Resource Bundles
```
$ mvn gp:upload
```
This goal does following tasks:

1. Scans files under `src/main/resources` and locates files with `.properties` extension
(but excluding files with '_'(underscore) character in its file name, such as
`Messages_fr.properties`).

2. For each Java property resource bundle file, checks if corresponding Globalization
Pipeline bundle already exists or not. If it's not available, creates a new bundle
with all translation target languages currently configured. English is used as the
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

#### Downloading Translated Resource Bundles
```
$ mvn gp:download
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

In many cases, you will likely want to execute the goal during `package` phase
and include translated resource bundles downloaded from an instance of Globalization
Pipeline service. To execute the goal automatically in `process-resources` lifecycle,
you can add `<execution>' element as below.

```
      <plugin>
        <groupId>com.ibm.g11n.pipeline</groupId>
        <artifactId>gp-maven-plugin</artifactId>
        <version>1.1.4</version>
        <executions>
            <execution>
                <goals>
                  <goal>download</goal>
                </goals>
            </execution>
        </executions>
      </plugin>
```

## <a name="TOC-Usage-Advanced"></a>Advanced Use Cases

Globalization Pipeline Maven Plugin supports various configuration parameters to customize
the behavior. This section provides some use cases with these configuration parameters.
Please see [Configuration Parameter Reference](#TOC-ConfigParamRef) for further information
about the configuration parameters.

### Java Property Resource Bundle Files in Multiple Locations

When you have translatable resource files not in the default location, you can use `<bundleSets>`
to specify the location. This configuration supports multiple different directories.

For example, you have translatable English *.properties files under `src/nls1/resources/en`
and `src/nls2/resources/en`. If you want to include both sets, use the configuration below.

```
    <configuration>
        <bundleSets>
            <bundleSet>
                <sourceFiles>
                    <directory>src/nls1/resources/en</directory>
                    <includes>
                        <include>**/*.properties</include>
                    </includes>
                </sourceFiles>
            </bundleSet>
            <bundleSet>
                <sourceFiles>
                    <directory>src/nls2/resources/en</directory>
                    <includes>
                        <include>**/*.properties</include>
                    </includes>
                </sourceFiles>
            </bundleSet>
        </bundleSets>
    <configuration>
```

### Custom Language ID Mapping

Globalization Pipeline service uses language ID "pt-BR" for Brazilian Portuguese, "zh-Hans" for
Simplified Chinese and "zh-Hant" for Traditional Chinese. You may want to use just "pt"
for Brazilian Portuguese, or "zh" for Simplified Chinese, "zh_TW" for Traditional Chinese
as a part of output file name (or path). The following example does such mapping.

```
    <configuration>
        <bundleSets>
            <bundleSet>
                <sourceFiles>
                    <directory>src/resources</directory>
                    <includes>
                        <include>**/*.properties</include>
                    </includes>
                    <excludes>
                        <exclude>**/*_*.properties</exclude>
                    </excludes>
                </sourceFiles>
                <languageMap>
                    <pt-BR>pt</pt-BR>
                    <zh-Hans>zh</zh-Hans>
                    <zh-Hant>zh-TW</zh-Hant>
                </languageMap>
            </bundleSet>
        </bundleSets>
    <configuration>
``` 
If you have properties file `src/resources/com/ibm/example/MyStrings.properties`,
`download` goal will generate Brazilian Portuguese translated file `target/classes/com/ibm/example/MyStrings_pt.properties`
instead of `target/classes/com/ibm/example/MyStrings_pt_BR.properties`

Note: The language ID separator used in the mapping should be '-' (Hyphen) for both,
even you want translated file name (or path) to use '_' for the language part. In the example
above, do not specify `<zh-Hant>zh_TW</zh-Hant>` even you want `MyStrings_zh_TW.properties`
as the output file name. The language ID style is configured by `<languageIdStyle>`.


### Translated JSON Files in Language Directories

Your project may need place localized JSON files in its own language directory.
Following example supports all JSON files in directory "en" and produces translated
versions in parallel language directories.

```
    <configuration>
        <bundleSets>
            <bundleSet>
                <sourceFiles>
                    <directory>src/main/webapp</directory>
                    <includes>
                        <include>**/en/*.json</include>
                    </includes>
                </sourceFiles>
                <type>JSON</type>
                <bundleLayout>LANGUAGE_DIR</bundleLayout>
                <languageIdStyle>BCP47</languageIdStyle>
                <outputDir>target/MyApp</outputDir>
            </bundleSet>
        </bundleSets>
    <configuration>

```
`<bundleLayout>` specifies file name/path mapping behavior. **LANGUAGE_DIR** specifies
localized version will use the same file name with source, but placed in a directory
for the language. `<languageIdStyle>` specifies the style of language ID used for
file/path name. **BCP47** specifies the BCP 47 language tag. In this case, '-' (Hyphen)
is used for subtag separtors, such as 'zh-Hans'.

With this example, if you have English JSON resource file at `src/main/webapp/res/en/MyStrings.json`,
French version is generated at `target/MyApp/res/fr/MyStrings.json`, and Simplified Chinese
version is generated at `target/MyApp/res/zh-Hans/MyStrings.json`.


# <a name="TOC-ConfigParamRef"></a>Configuration Parameter Reference

### `<credentials>`

Specifies Globalization Pipeline service credentials. There are 4 nested elements
below.

* `<url>` REST service URL, for example, `https://gp-rest.ng.bluemix.net/translate/rest`
* `<userId>` User ID
* `<password>` Password
* `<instanceId>` Service instance ID

See [Specifying Globalization Pipeline Service Credentials](#TOC-Usage-Credentials) for
further information.

### `<outputDir>`

Specifies the output base directory used by `download` goal.
This configuration parameter is also available in `<bundleSet>` element (see the section below),
which overrides this configuration for the specified subset.

The default value is the standard Java class file output directory (`target/classes`).


### `<overwrite>`

Specifies a boolean value to control whether `download` goal overwrites translated resource bundle files
already in the output directory or not.
The default value "true"


### `<bundleSets>`

`<bundleSets>` specifies sets of resource bundle files to be used as translation source bundles.
Each set is defined by `<bundleSet>` elements. The next example illustrates how multiple `<bundleSet>`
elements can be used.

```
    <bundleSets>
        <bundleSet>
            <sourceFiles>
                <directory>src/main/resources</directory>
                <includes>
                    <include>**/*.properties</include>
                </includes>
            </sourceFiles>
            <type>JAVA</type>
            [...]
        </bundleSet>
        <bundleSet>
            <sourceFiles>
                <directory>src/resources/jsonres</directory>
                <includes>
                    <include>**/nls.json</include>
                </includes>
            </sourceFiles>
            <type>JSON</type>
            <bundleLayout></bundleLayout>
            [...]
        </bundleSet>
        [...]
    </bundleSets>
```

For each `<bundleSet>`, you can use following configuration parameters.

#### `<sourceFiles>` (required)

Specifies a set of source bundle files.

* `<directory>` (required) Base directory
* `<includes>` Sets for inclusion rules specified by nested `<include>` elements
  * `<include>` Specifies a single rule for inclusion
* `<excludes>` Sets for exclusion rules specified by nested `<exclude>` elements
  * `<exclude>` Specifies a single rule for exclusion

The inclusion/exclusion rules use the standard ant/maven fileset syntax. For example,

```
    <sourceFiles>
        <directory>src/MyResources</directory>
        <includes>
            <include>**/*.json</include>
        </includes>
        <excludes>
            <exclude>**/config.json</exclude>
        </excludes>
    </sourceFiles>
```
Above example includes all `*.json` files under `src/MyResources` directory, but
excludes all files with file name `config.json`.

#### `<type>`

Specifies a resource type. Available options are

* **JAVA** - Java property resource bundle file
* **JSON** - Resource string key/value pairs stored in JSON format. For now nested JSON object is not supported.
* **AMDJS** - RequireJS I18N bundle file
* **IOS** - iOS String Resource file
* **ANDROID** - Android String Resource file
* **PO** - GNU Gettext portable object file
* **POT** - GNU Gettext portable object template file
* **YML** - YAML resource bundle file
* **XLIFF** - XLIFF file (Not fully implemented)

The default value is **JAVA**.

#### `<sourceLanguage>`

Specifies BCP 47 language tag for the language used in the source bundles.
The default value is "en" (English).

#### `<targetLanguages>`

Specifies a list of translation target languages by BCP 47 language tags. Each target language
must be enclosed by `<param>` element. For example, if you want to set French ("fr"), German ("de")
and Italian ("it") as target languages, the parameter should be specified as below:

```
    <targetLanguages>
        <param>fr</param>
        <param>de</param>
        <param>it</param>
    </targetLanguages>
```

If this parameter is not specified, `upload` goal creates a new Globalization Pipeline bundle
with all available machine translation target languages, and `download` goal exports all
target languages currently available in the Globalization Pipeline bundle.

#### `<languageMap>`

Specifies custom language mappings. Each nested element name is a [BCP 47 language tag](https://tools.ietf.org/html/bcp47)
used by Globalization Pipeline service instance, and the element value is a language ID used for
output resource file or path name. For example, Globalization Pipeline service uses language ID
"pt-BR" for Brazilian Portuguese. If you want to use just "pt" for output file or path name, you
can specify the mapping as below.

```
    <languageMap>
        <pt-BR>pt</pt-BR>
    </languageMap>
```

There are no custom language ID mappings by default.

#### `<languageIdStyle>`

Specifies one of following keywords to configure the rule for composing language ID used
for output resource bundle file or path name.

* **BCP47_UNDERSCORE** BCP 47 language tag, replacing '-' with '_'. For example, *zh_Hant* for Traditional Chinese.
* **BCP47** BCP 47 language tag itself. For example, *zh-Hant* for Traditional Chinese

The default value is **BCP47_UNDERSCORE**.

#### `<outputDir>`

Specifies the output base directory for this `<bundleSet>`.
If not specified, `<outputDir>` just under `<configuration>` for this plugin will be used.

#### `<outputSourceLanguage>`

Specifies a boolean value to control whether `download` goal also produces resource bundle
files for a source language.
The default value is "false".

#### `<outputContentOption>`

Specifies one of following keywords to control how `download` goal generates the contents of
translated resource bundle files.

* **MERGE_TO_SOURCE** Duplicates the contents of the source bundle and replaces only translated
resource strings. This option might not be implemented by some format types. In this case,
**TRANSLATED_WITH_FALLBACK** is used instead.
* **TRANSLATED_WITH_FALLBACK** Emits only resource strings (with a simple header if applicable).
When translated string value is not available, the value in the source language is used.
* **TRANSLATED_ONLY** Emits only resource strings (with a simple header if applicable).
When translated string value is not available, do not include the key in the output.

The default value is **MERGE_TO_SOURCE**.

#### `<bundleLayout>`

Specifies one of following keywords to control output file name or path in `download` goal.

* **LANGUAGE_SUFFIX** In the same directory with the source bundle file, with extra language suffix.
For example, if the source bundle file is `com/ibm/g11n/MyMessages.properties`, then the French
version will be `com/ibm/g11n/MyMessages_fr.properties`.
* **LANGUAGE_SUBDIR** In a language sub-directory under the directory where the source bundle file
is placed. For example, if the source bundle file is `res/MyMessages.json`, then the French
version will be `res/fr/MyMessages.json`.
* **LANGUAGE_DIR** In a language directory at the same level with the source bundle file.
For example, if the source bundle file is `com/ibm/g11n/en/MyMessages.properties`,
then the French version will be `com/ibm/g11n/fr/MyMessages.properties`.

The default value is **LANGUAGE_SUFFIX**.


### Default Configuration

The configuration below is used when `<configuration>` element is not explicitly specified in
pom.xml

```
    <configuration>
        <bundleSets>
            <bundleSet>
                <sourceFiles>
                    <directory>src/main/resources</directory>
                    <includes>
                        <include>**/*.properties</include>
                    </includes>
                    <excludes>
                        <exclude>**/*_*.properties<exclude>
                    </excludes>
                </sourceFiles>
                <type>JAVA</type>
                <sourceLanguage>en</sourceLanguage>
                <languageIdStyle>BCP47_UNDERSCORE</languageIdStyle>
                <outputSourceLanguage>false</outputSourceLanguage>
                <outputContentOption>MERGE_TO_SOURCE</outputContentOption>
                <bundleLayout>LANGUAGE_SUFFIX</bundleLayout>
            </bundleSet>
        </bundleSets>
        <outputDir>target/classes</outputDir>
        <overwrite>true</overwrite>
    </configuration>
```
