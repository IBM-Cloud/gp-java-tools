/*
 * Copyright IBM Corp. 2015
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
package com.ibm.g11n.pipeline.resfilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.BreakIterator;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.TreeSet;

import com.ibm.g11n.pipeline.resfilter.ResourceString.ResourceStringComparator;

public class IOSStringsResource implements ResourceFilter {

    private static final String CHARSET_STRING = "UTF-8";

    @Override
    public Collection<ResourceString> parse(InputStream in) throws IOException {
        Collection<ResourceString> resultCol = new LinkedList<ResourceString>();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String temp = "";
        int sequenceNum = 0;
        while ((temp = br.readLine()) != null) {
            int index;
            // looking for key = value
            if ((index = temp.indexOf("=")) != -1) {

                StringBuilder valueString = new StringBuilder(100);
                String key = temp.substring(0, index).trim().replace("\"", "");
                String value = temp.substring(index + 1).trim().replace(";", "").replace("\"", "").replaceAll("%@ *",
                        "");
                valueString.append(value);

                while (temp.indexOf(";") == -1) {
                    // TODO: null handling?
                    temp = br.readLine();
                    valueString.append(temp.trim().replace(";", "").replaceAll("%@\n *", ""));
                }
                ResourceString res = new ResourceString(key, valueString.toString());
                sequenceNum++;
                res.setSequenceNumber(sequenceNum);
                resultCol.add(res);
            }
        }

        return resultCol;
    }

    @Override
    public void write(OutputStream os, String language, Collection<ResourceString> resStrings) throws IOException {
        TreeSet<ResourceString> sortedResources = new TreeSet<>(new ResourceStringComparator());
        sortedResources.addAll(resStrings);

        StringBuilder temp = new StringBuilder(100);
        for (ResourceString res : sortedResources) {
            // empties the buffer
            temp.setLength(0);
            temp.append("\"").append(res.getKey()).append("\"");
            temp.append(" = ");
            temp.append("\"").append(res.getValue()).append("\";");
            temp.append("\n");
            os.write(temp.toString().getBytes(Charset.forName(CHARSET_STRING)));
        }
    }

    @Override
    public void merge(InputStream base, OutputStream os, String language, Collection<ResourceString> resStrings) throws IOException {
        Scanner in = new Scanner(base, "UTF-8");
        String line = "";
        while (in.hasNextLine()) {
            line = in.nextLine() + '\n';
            if (line.indexOf("=") == -1) {
                os.write(line.getBytes());
            } else {
                String key = line.substring(0, line.indexOf("=")).trim();
                for (ResourceString res : resStrings) {
                    if (res.getKey() == key) {
                        StringBuilder temp = new StringBuilder(100);
                        temp.append("\"").append(key).append("=").append("\"");

                        final int character_offset = 80;
                        BreakIterator b = BreakIterator.getWordInstance();
                        b.setText(res.getValue());

                        int offset = 80;
                        int start = 0;

                        boolean first = true;

                        while (start < res.getValue().length()) {
                            if (res.getValue().length() > character_offset) {
                                if (first) {
                                    temp.append("\"");
                                }
                                if (!first) {
                                    temp.append(" ");
                                }

                                first = false;
                                int end = b.following(offset);
                                String str = res.getValue().substring(start, end);
                                start = end;
                                offset += 80;
                                temp.append(str).append(" %@\n");
                            } else {
                                temp.append("\"").append(res.getValue());
                                start = res.getValue().length();
                            }
                        }

                        temp.append("\";");
                        os.write(temp.toString().getBytes(Charset.forName(CHARSET_STRING)));
                    } else {
                        os.write(line.getBytes());
                    }
                }
            }
        }
        in.close();
    }
}
