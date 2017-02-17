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
Java Client Tools for Globalization Pipeline on IBM Bluemix
==

# What is this?

This repository contains Java tools for
[Globalization Pipeline on IBM Bluemix](https://www.ng.bluemix.net/docs/services/GlobalizationPipeline/index.html).

[![Build Status](https://travis-ci.org/IBM-Bluemix/gp-java-tools.svg?branch=master)](https://travis-ci.org/IBM-Bluemix/gp-java-tools)
[![Coverage Status](https://coveralls.io/repos/github/IBM-Bluemix/gp-java-tools/badge.svg?branch=master)](https://coveralls.io/github/IBM-Bluemix/gp-java-tools?branch=master)
[![Coverity Scan](https://img.shields.io/coverity/scan/9398.svg)](https://scan.coverity.com/projects/ibm-bluemix-gp-java-tools)

[![gp-java-tools](https://img.shields.io/maven-central/v/com.ibm.g11n.pipeline/gp-java-tools.svg)](#)


# Getting started

To get started, you should familiarize yourself with the service itself. A good place
to begin is by reading the [Quick Start Guide](https://github.com/IBM-Bluemix/gp-common#quick-start-guide) and the official [Getting Started with IBM Globalization ](https://www.ng.bluemix.net/docs/services/GlobalizationPipeline/index.html)
documentation.

# Usage

## Command Line Interface (gp-cli)

This tool provides command line interface for accessing resource bundles and user
information. For example, exporting a resource bundle hosted by an instance
of Globalization Pipeline service to a specific file format, adding a new target
language in bundle configuration, creating a new user and so on.

To see available commands and options, please see [Globalization Pipeline CLI Tool User Guide](gp-cli/README.md)

[![gp-cli](https://img.shields.io/maven-central/v/com.ibm.g11n.pipeline/gp-cli.svg)](#)


## Maven Plugin (gp-maven-plugin)

This plugin integrates Globalization Pipeline service with an Apache Maven build.
With the plugin, you can upload source resource bundles to your own Globalization
Pipeline service instance, and download translated resource bundles.

Please see [Globalization Pipeline Maven Plugin User Guide](gp-maven-plugin/README.md) for further
information.

[![gp-maven-plugin](https://img.shields.io/maven-central/v/com.ibm.g11n.pipeline/gp-maven-plugin.svg)](#)


## Ant Task (gp-ant-task)

This custom task library integrates Globalization Pipeline service with an Apache
Ant build. With the custom tasks provided in this library, you can upload source resource
bundles to your own Globalization Pipeline service instance, and download translated
resource bundles.

Please see [Globalization Pipeline Ant Task User Guide](gp-ant-task/README.md) for further
information.

[![gp-ant-task](https://img.shields.io/maven-central/v/com.ibm.g11n.pipeline/gp-ant-task.svg)](#)


# Community

* View or file GitHub [Issues](https://github.com/IBM-Bluemix/gp-java-tools/issues)
* Connect with the open source community on [developerWorks Open](https://developer.ibm.com/open/ibm-bluemix-globalization-pipeline-service/)

# Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

# License

Apache 2.0. See [LICENSE.txt](LICENSE.txt).

> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this file except in compliance with the License.
> You may obtain a copy of the License at
>
> http://www.apache.org/licenses/LICENSE-2.0
>
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.

