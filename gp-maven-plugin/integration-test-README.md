## gp-maven-plugin Integration Tests

To test existing features of gp-maven-plugin, integration tests have been added. New integration tests can be added 
to test more advanced cases/new features as per need by following the template of existing integration tests. To execute the integration tests:

* Make sure that following profile is added to the **src/pom.xml**
```xml
<profile>
  <id>run-its</id>
    <activation>
      <property>
        <name>skipTests</name>
          <value>!true</value>
      </property>
    </activation>
    <build>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-invoker-plugin</artifactId>
					<version>2.0.0</version>
					<configuration>
						<debug>true</debug>
						<addTestClassPath>true</addTestClassPath>
						<cloneProjectsTo>${project.build.directory}/it</cloneProjectsTo>
						<setupIncludes>
							<setupInclude>setup/pom.xml</setupInclude>
						</setupIncludes>
						<pomIncludes>
							<pomInclude>*/pom.xml</pomInclude>
						</pomIncludes>
						<preBuildHookScript>setup</preBuildHookScript>
						<postBuildHookScript>verify</postBuildHookScript>
						<localRepositoryPath>${project.build.directory}/local-repo</localRepositoryPath>
						<settingsFile>src/it/settings.xml</settingsFile>
					</configuration>
					<executions>
            <execution>
              <id>integration-test</id>
              <goals>
                <goal>run</goal>
              </goals>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </build>
  </profile>
```
* Then execute ```$ mvn integration-test ```
  
  The result of this run will show which tests passed/failed
  
## Integration Test Directory Structure

All the integration tests are organized into folders under **src/it** directory. The directory structure is shown below:

```
src
├── it
|   ├── settings.xml
|   ├── t1-basic-upload-fail
|   ├── t2-basic-upload-success
|   ├── t3-basic-download-fail
|   └── t4-basic-download-success
|       ├── src/main/resources/com/bundle1
|       |                          ├── ResourceBundle.properties
|       |                          └── ResourceBundle.json
|       ├── invoker.properties
|       ├── pom.xml
|       ├── setup.properties
|       ├── setup.groovy
|       └── verify.groovy
└── test
    └── java
        └── com
            └── ibm
               └── g11n
                  └── pipeline
                      └── maven
                          ├── CredentialsExtended.java
                          ├── GPInstance.java
                          └── ITTools.java
```

Let's take the example of the integration test **_t4-basic-download-success_**

This integration test ensures that the download of json resource bundle files should succeed

The **_t4-basic-download-success_** test contains following files:

####invoker.properties

> This file contains the properties for the integration test invoker.
```
invoker.name=basic download test which should succeed
invoker.goals=${project.groupId}:${project.artifactId}:${project.version}:upload integration-test
invoker.debug=false
invoker.buildResult=success
invoker.mavenOpts=-Dgp.credentials.json=credentials.json
```
> The ```invoker.goals``` specifies the gp-maven-plugin action that needs to be triggered as part of integration test

> The ```invoker.buildResult``` attribute specifies the expected result of the test (_success_/_failure_)

> The ```invoker.mavenOpts``` specifies the credentials file which needs to be used before triggering the goal

> The file is **mandatory**

####pom.xml

> This file specifies the plugin execution goals as desired.
```xml
  <plugin>
    <groupId>@project.groupId@</groupId>
    <artifactId>@project.artifactId@</artifactId>
    <version>@project.version@</version>
    <executions>
      <execution>
        <goals>
          <goal>download</goal>
        </goals>
      </execution>
    </executions>
    <configuration>
      <bundleSets>
        <bundleSet>
          <sourceFiles>
            <directory>src/main/resources</directory>
              <includes>
                <include>**/*.json</include>
              </includes>
          </sourceFiles>
          <type>JSON</type>
        </bundleSet>			            
      </bundleSets>
    </configuration>
  </plugin>
```
> The above shown plugin execution configuration implies that the gp-maven-plugin should execute download as part of the maven build.

> The integration test should download all the ```json``` files from globalization pipeline instance as per the bundles 
located in the ```src/main/resources``` and put all the downloaded files and organize them in the default output location
of ```target/classes```

> The file is **mandatory** but can omit plugin configuration if not required for the integration test

####setup.groovy

> This is the prebuild hook script as configured in the ```src/pom.xml```. 

> This speficies the setup activities that need to be performed before executing the build for the integration test

> For this test, the script creates a ```credentials.json``` file which contains credentials of a shortlived globalization
pipeline instance for integration test purposes

> The script makes use of methods from ```ITTools.java```. This was made possible because ```<addTestClassPath>true</addTestClasPath>```
was added to **src/pom.xml** as part of ```maven-invoker-plugin``` configuration

> This is **optional** if there is no setup required for a particular integration test

####verify.groovy

> This is the postbuild hook script as configured in the ```src/pom.xml```

> This specifies the verification activities that needs to be performed post build (as part of integration test), 
to make sure that the build gave the results as expected.

> This is **optional** if there are no verification activities that need to be performed post build (integration-test)

####setup.properties

> This file is **optional** and is basically used here to help with build setup as implemented in ```setup.groovy``` script.

####src/main/resources/com/bundle1/

> This directory and its content are relevant to this particular integration test only.

##Invoking the integration test
To just execute ```t4-basic-download-success``` test following command can be issued

```$ mvn integration-test -Dinvoker.test=t4-basic-download-success```

* As the test is executed, first the repository is cloned to a local repository (as specified in **src/pom.xml**)

* All the dependency artifacts are downloaded to the local-repo

* The prebuild script is then run to prepare setup before starting the build. For this test, the script generates ```credentials.json```

* The invoker goals are then executed (using the ```invoker.mavenOpts``` if supplied).
  For this test, first the ```upload``` goal is triggered and then ```download```
  
* The postbuild script then runs to verify that the build was successful with desired output.
  For this test, the downloaded files are examined to verify the number of files and also the number of translated keys per file.




