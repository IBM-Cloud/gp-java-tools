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
# Custom Resource Filter Example - CSV Filter

[Globalization Pipeline Resource Filter](https://github.com/IBM-Cloud/gp-java-tools/tree/master/gp-res-filter) provides
a mechanism to implement your own custom filter, that can be used through
Globalization Pipeline Maven Plugin/Ant Task and other tools.

This example filter implementation read and write resource strings in CSV (Comman Separated Version) format.

There are two types of resource filters.

- [ResourceFilter](https://github.com/IBM-Cloud/gp-java-tools/blob/master/gp-res-filter/src/main/java/com/ibm/g11n/pipeline/resfilter/ResourceFilter.java) :
This filter type parses a single resource data and produce a single Globalization
Pipeline bundle. It also writes out a single Globalization Pipeline bundle into a
single resource data.
- [MultiBundleResourceFilter](https://github.com/IBM-Cloud/gp-java-tools/blob/master/gp-res-filter/src/main/java/com/ibm/g11n/pipeline/resfilter/MultiBundleResourceFilter.java) :
This filter type parses a single resource data and produces one ore more Globalization
Pipeline bundles. It also writes out one or more Globalization Pipeline bundles
into a single resource data.

A custom resource filter must extends one of above abstract class. Once you have
one or more filter implementations ready, you need to impelment a concrete subclass
of [ResourceFilterProvider](https://github.com/IBM-Cloud/gp-java-tools/blob/master/gp-res-filter/src/main/java/com/ibm/g11n/pipeline/resfilter/ResourceFilterProvider.java).

In this example, [CSVFilter](https://github.com/IBM-Cloud/gp-java-tools/blob/master/csv-res-filter/src/main/java/com/ibm/g11n/pipeline/CSVFilter.java)
extends `ResourceFilter` and [MultiBundleCSVFilter](https://github.com/IBM-Cloud/gp-java-tools/blob/master/csv-res-filter/src/main/java/com/ibm/g11n/pipeline/MultiBundleCSVFilter.java) extends `MultiBundleResourceFilter`.

To integrate your custom filter implementation, you must create a text file
`com.ibm.g11n.pipeline.resfilter.ResourceFilterProvider` specifying your
`ResourceFilterProvider` implementation class, then place the file under
`META-INF/services` in a jar file. In this CSV filter example, the service
description file is found [here](https://github.com/IBM-Cloud/gp-java-tools/blob/master/csv-res-filter/src/main/resources/META-INF/services/com.ibm.g11n.pipeline.resfilter.ResourceFilterProvider).

Once your custom filter jar file is ready, you just need to put the file in
your runtime class path of Globalization Pipeline tools integrated with
`gp-res-filter`.
