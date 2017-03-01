# Globalization Pipeline Ant Task Example

This directory contains example Ant build.xml illustrating how
Globalization Pipeline Ant Task works.

Please follow the steps below to set up the environment.

1. Configure Appache Ant 1.9 or later version.
2. Download **gp-ant-task-X.X.X-with-dependencies.jar** from GitHub
[gp-java-tools release page](https://github.com/IBM-Bluemix/gp-java-tools/releases).
Copy the downloaded jar file to this (example) directory and rename jar file to
**gp-ant-task.jar**.
3. Edit **creds.json** in this directory and put actual Globalization Pipeline
service credentials.

There are 4 example targets (and `clean`) defined in the example build.xml.
You can try `> ant upload-props` to upload the contents of Java property files in the
specified directory to your Globalization Pipeline instance.
Then try `> ant download-props` to see how translated property files are downloaded.
