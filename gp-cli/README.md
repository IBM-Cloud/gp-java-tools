# Globalization Pipeline CLI Tool User Guide
<!--
/*  
 * Copyright IBM Corp. 2016
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
* [Command Reference](#TOC-Command-Reference)
  * [Help Command](#TOC-Cmd-Help)
  * [Bundle Commands](#TOC-Cmd-Bundle)
  * [User Commands](#TOC-Cmd-User)
  * [Other Commands](#TOC-Cmd-Others)
* [How-To](#TOC-How-To)
  * [Import Existing Translations](#TOC-Import-Existing-Translations)

---
## <a name="TOC-Overview"></a>Overview

Globalization Pipeline CLI (Command Line Interface) Tool is designed for
manipulating translation bundles hosted by Globalization Pipeline service
on command line.

---
## <a name="TOC-Prerequisites"></a>Prerequisites

Globalization Pipeline CLI Tool is distributed in a single jar package.
You need Java SE Runtime Environment 7 or later version to run the tool.

---
## <a name="TOC-Command-Reference"></a>Command Reference

Most of commands takes user credentials for a Globalization Pipeline service
instance. The common command options for specifying user credentials are
below.

* -s (--serviceUrl): Globalization Pipeline service URL
* -i (--instanceId): Service instance ID
* -u (--user): User ID
* -p (--password): Password

For example,
```
java -jar gp-cli.jar list-bundle -s https://gp-rest.ng.bluemix.net/translate/rest -i 1d42b9329d6f5ab173c82810c819afa6 -u 8492e8d7bc28d9dc34a31a6d0ec7384e -p DoRfuq1w1ohx8vKXoZHHMenxxpSyoF0u
```
You can access these credentials on Bluemix Dashboard. For more details,
plese see [Credentials](https://github.com/IBM-Bluemix/gp-common/blob/master/README.md#4-credentials)
section in the Globalization Pipeline Quick Start Guide.

Alternatively, you can store the credentials in a JSON file and use -j (--jsonCreds)
option to specify the file. For example, you can create a JSON file `mycreds.json'
with the contents below (Note: actual property values should match the actual service
instance's credentials):
```
{
    "url": "https://gp-rest.ng.bluemix.net/translate/rest",
    "userId": "8492e8d7bc28d9dc34a31a6d0ec7384e",
    "password": "DoRfuq1w1ohx8vKXoZHHMenxxpSyoF0u",
    "instanceId": "1d42b9329d6f5ab173c82810c819afa6"
}
```
With -j option, following command is equivalent to the above example.
```
java -jar gp-cli.jar list-bundle -j mycreds.json
```
---
### <a name="TOC-Cmd-Help"></a>Help Command

The help command prints out all commands and available options.
```
java -jar gp-cli.jar help
```
---
### <a name="TOC-Cmd-Bundle"></a>Bundle Commands

#### list (list-bundle)

Prints out a list of bundle IDs available in the service instance.
```
java -jar gp-cli.jar list -j mycreds.json
```
Below is an example output
```
["MyBundle1", "MyBundle2"]
```

#### show (show-bundle)

Prints out a bundle's summary information. The following example shows
the information of bundle *MyBundle1*.
```
java -jar gp-cli.jar show -b MyBundle1 -j mycreds.json
```
Below is an example output.
```
{
  "sourceLanguage": "en",
  "targetLanguages": [
    "de",
    "es",
    "fr",
    "it",
    "ja",
    "ko",
    "pt-BR",
    "zh-Hans",
    "zh-Hant"
  ],
  "readOnly": false,
  "updatedBy": "(dash+3f4bb9f12d6f5db965482811c81ca215)mike@acme.com",
  "updatedAt": "2016-07-07T12:02:34.788-04"
}
```

#### create (create-bundle)

Creates a new bundle. This command creates an empty bundle with
specified bundle ID and languages.

The following example creates
a new bundle with ID *MyNewBundle* with English(en) as source language,
and French(fr) and German(de) as target languages. The first language
specified in -l option is the source language, and the rest of languages
are the translation target languages.
```
java -jar gp-cli.jar create -b MyNewBundle -l en,fr,de -j mycreds.json
```

#### update (update-bundle)

Updates an existing bundle's configuration.

The following example sets French (fr), German (de) and Italian (it)
as target languages and the translation instruction note "These are
Java MessageFormat ...".
```
java -jar gp-cli.jar update -b MyBundle -l fr,de,it -n "These are Java MessageFormat pattern strings"
```

#### import

Imports resource data to a bundle. This command takes an input language.
If the language does not exist in the bundle, the language will be
automatically added to the bundle configuration.

There are several different resource types available:

* **JAVA** - Java property resource bundle file
* **JSON** - Resource string key/value pairs stored in JSON format. For now nested JSON object is not supported.
* **AMDJS** - RequireJS I18N bundle file
* **GLOBALIZEJS** - Globalize.js JSON resource bundle file
* **IOS** - iOS String Resource file
* **ANDROID** - Android String Resource file
* **PO** - GNU Gettext portable object file
* **POT** - GNU Gettext portable object template file
* **YML** - YAML resoruce bundle file
* **XLIFF** - XLIFF 1.2 file

For example, the followng example imports English (en) resource strings in Java properties
file *MyBundle.properties* to the bundle *MyBundle*.
```
java -jar gp-cli.jar import -b MyBundle -l en -t JAVA -f MyBundle.properties -j mycreds.json
```

#### export

Exports the contents of bundle to a resource bundle file. The supported resource
bundle types are same with the import command.

For example, the following example exports French (fr) resource strings from the bundle
*MyBundle* to the Java property file *MyBundle_fr.properties*.
```
java -jar gp-cli.jar export -b MyBundle -l fr -t JAVA -f MyBundle_fr.properties
```

If you have the original (source) resource bundle file, and you want to keep the
original format including the order of resource keys, comments and others, -o option might
be used. This feature is not supported by some resoruce types such as JSON.

When translated resource string value is not available, the export command does not
include the resource key in the output by default. If you want to include such resource
key with the value from the source language, you can specify -k option.


#### delete (delete-bundle)

Deletes a bundle. The following example deletes a bundle *MyBundle* from
your service instance.
```
java -jar gp-cli.jar delete -b MyBundle -j mycreds.json
```

#### copy (copy-bundle)

Creates a copy of the specified bundle. This command allows you to duplicates a
bundle in the same service instance, or another service instance.
The following example copies the contents of exiting bundle *MyBundle* to
a new bundle *MyNewBundle* in the same service instance.
```
java -jar gp-cli.jar copy -b MyBundle -d MyNewBundle -j mycreds.json
```
This command can be used to duplicate a bundle from a service instance to
another service instance. In this case, you need to specify the destination instance's
credentials with --dest-* options as below.
```
java -jar gp-cli.jar copy -b MyBundle -d MyBundle --dest-url https://gp-rest.ng.bluemix.net/translate/rest --dest-instance-id 9146abf71bb94513504a0eaf76d57804 --dest-user-id 52858e19ae57ba6f2d2ea7e38e9ab457 --dest-password o75YXQCK2obQLOvedkSslBTAyeUq7/+t -j mycreds.json
```
Note: This command does not copy service managed properties, such as
updatedBy and updatedAt stored in each entity. The newly copied bundle
and resource entries will have new timestamp in updatedAt property
and the operator of this command will be set to updatedBy property.


#### copy-all-bundles

Copies all bundles from a service instance to another. This command invokes
*copy (copy-bundle)* operation for all bundles in the specified service
instance. This command is convenient for moving all bundle data to a newly
created instance.
The following example copies all bundles in the service instance to
another instance specified by --dest-instance-id.
```
java -jar gp-cli.jar copy-all-bundles --dest-url https://gp-rest.ng.bluemix.net/translate/rest --dest-instance-id 9146abf71bb94513504a0eaf76d57804 --dest-user-id 52858e19ae57ba6f2d2ea7e38e9ab457 --dest-password o75YXQCK2obQLOvedkSslBTAyeUq7/+t -j mycreds.json
```
Note: This command copies bundle data to the destination service instance,
but user accounts and translation configurations are not transferred, because
they are service instance specific.


---
### <a name="TOC-Cmd-User"></a>User Commands

#### list-users

Prints out a list of users in the service instance.
```
java -jar gp-cli.jar list-users -j mycreds.json
```


#### create-user

Creates a new user in the service instance.

The example below creates a new
user account with read-only access to the bundles *MyAppsBundle1* and
*MyAppsBundle2*.
```
java -jar gp-cli.jar create-user -t READER -b MyAppsBundle1,MyAppsBundle2 -j mycreds.json
```
The output of above command looks like below.
```
A new user was successfully created.
User ID: 9b8b65c31cf56a8eac2bd4a1a1f09175
User password: IpBiOP0s4xXs3SRSgofYLLasU6/qEWFf
```
Note: The password above cannot be retrieved from the service later.
If you lost the password, you can only reset the password by
*reset-user-password* command below.

The command option -t specifies a type of new user. There are
following user types.
* **ADMINISTRATOR** - Administrator of the service instance.
* **TRANSLATOR** - Can read bundles and edit tranlations. This account type
is designed for external tooling for translators.
* **READER** - Can only read basic information of bundles and resource strings.
This account type is designed for distributing credentials included in a client
application accessing the translated strings dynamically.


#### reset-user-password

Resets a user's password. The example below resets the password of the
user whose ID is 9b8b65c31cf56a8eac2bd4a1a1f09175.
```
java -jar gp-cli.jar reset-user-password -d 9b8b65c31cf56a8eac2bd4a1a1f09175 -j mycreds.json
```
The out of above command looks like below.
```
New password: CD0ZPBvOoTCZbDCDssY/sMEjmTvW+rYs
```


#### delete-user

Deletes a user. The example below delets the user whose ID is
9b8b65c31cf56a8eac2bd4a1a1f09175.
```
java -jar gp-cli.jar delete-user -d 9b8b65c31cf56a8eac2bd4a1a1f09175 -j mycreds.json
```

---
### <a name="TOC-Cmd-Others"></a>Other Commands

#### list-mt-languages

Prints out machince translation languages currently enabled in the
service instance. For example, the following example prints out an array
of machine translation target languages from English (en).
```
java -jar gp-cli.jar list-mt-languages -f en -j mycreds.json
```
The output of above command looks like below.
```
["de","es","fr","it","ja","ko","pt-BR","zh-Hans","zh-Hant"]
```

---
## <a name="TOC-How-To"></a>How-To

### <a name="TOC-Import-Existing-Translations"></a>Import Existing Translations

You may already have resource bundles translated from English to several
different languages. If you want to import the already translated contents to
a Globalization Pipeline service bundle, then you can follow the steps below.

1\. Create a new bundle with English as the source language. In this example,
we use *com.ibm.example.MyBundle* as the bundle ID.
```
java -jar gp-cli.jar create -b com.ibm.example.MyBundle -l en -j mycreds.json
```

2\. Import the English(en) resource bundle contents to *MyBundle*. The resource bundle
file in this example is a Java properties file - MyBundle.properties
```
java -jar gp-cli.jar import -b com.ibm.example.MyBundle -l en -t JAVA -f MyBundle.properties -j mycreds.json
```
At this point, the bundle *com.ibm.example.MyBundle* in the Globalization
Pipeline service instance contains only English resource strings.

3\. Import the corresponding translated version. In this example, the operation
below imports French(fr) translation from MyBundle_fr.properties and marks
resource strings as already reviewed (-r).
```
java -jar gp-cli.jar import -b com.ibm.example.MyBundle -l fr -t JAVA -f MyBundle_fr.properties -j mycreds.json -r
```
This operation automatically adds French to the bundle *com.ibm.example.MyBundle*.

4\. Repeat step 3 above for other translated versions.





