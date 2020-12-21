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

package com.ibm.g11n.pipeline.tools.cli;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FakebrokerMgr {

    public static String FB_FILE = System.getProperty("GP_FAKEBROKER_FILE", "../test-fakebroker.json");
    public static String GPCONFIG_FILE = System.getProperty("GP_CONFIG_FILE", "../test-gpconfig.json");

    public static File getConfigFile() {
        final File gpconfig = new File(GPCONFIG_FILE);
        final File fbfile = new File(FB_FILE);
        if(!gpconfig.isFile()) {
            if(fbfile.isFile()) {
                try (final Reader reader = new FileReader(fbfile)) {
                    JsonElement parse = new JsonParser().parse(reader);
                    JsonObject o = parse.getAsJsonObject();
                    JsonObject creds = o.get("credentials").getAsJsonObject();
                    try (final Writer writer = new FileWriter(gpconfig)) {
                        writer.write(creds.toString()); // ?
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Could not convert " + fbfile.getAbsolutePath() + " to "
                            + gpconfig.getAbsolutePath());
                    return null;
                }
                // convert fbFile to gpconfig
                System.err.println("Converted " + fbfile.getAbsolutePath() + " to "
                        + gpconfig.getAbsolutePath());
                return gpconfig;
            } else {
                System.err.println("no fakebroker file:" + fbfile.getAbsolutePath());
                return null;
            }
        } else {
            // System.out.println("OK: " + gpconfig.getAbsolutePath());
            return gpconfig;
        }
    }

    public static void run(String... args) {
        final File gpconfig = getConfigFile();
        assumeNotNull(gpconfig); // skip test if not set
        assertTrue("Usage: run(\"list\", …) - arg count " + args.length, args.length > 0);
        final List<String> newArgs = new LinkedList<String>();
        final String verb = args[0];
        newArgs.add(verb); // verb
        newArgs.add("-j");
        newArgs.add(gpconfig.getAbsolutePath());
        for (int i = 1; i < args.length; i++) {
            newArgs.add(args[i]); // add the rest of the args
        }
        String[] asArray = newArgs.toArray(new String[0]);
        System.out.println("GPCmd: " + asArray);
        GPCmd.main(asArray);
    }

}
