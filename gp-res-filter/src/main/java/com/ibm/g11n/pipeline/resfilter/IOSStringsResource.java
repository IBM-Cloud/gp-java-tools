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
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class IOSStringsResource implements ResourceFilter {

    private static final String CHARSET_STRING = "UTF-8";

    @Override
    public Map<String, String> parse(InputStream in) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String temp = "";
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
                    temp = br.readLine();
                    valueString.append(temp.trim().replace(";", "").replaceAll("%@\n *", ""));
                }

                map.put(key, valueString.toString());
            }
        }

        return map;
    }

    @Override
    public void write(OutputStream os, String language, Map<String, String> map) throws IOException {
        StringBuilder temp = new StringBuilder(100);
        for (String key : map.keySet()) {
            // empties the buffer
            temp.setLength(0);

            String value = map.get(key);
            temp.append(key).append("=").append(value).append("\n");
            os.write(temp.toString().getBytes(Charset.forName(CHARSET_STRING)));
        }
    }

    @Override
    public void merge(InputStream base, OutputStream os, String language, Map<String, String> map) throws IOException {
        Scanner in = new Scanner(base, "UTF-8");
        String line = "";
        while (in.hasNextLine()) {
            line = in.nextLine() + '\n';
            if (line.indexOf("=") == -1) {
                os.write(line.getBytes());
            } else {
                String key = line.substring(0, line.indexOf("=")).trim();
                if (map.containsKey(key)) {
                    StringBuilder temp = new StringBuilder(100);
                    temp.append("\"").append(key).append("=").append("\"");

                    final int character_offset = 80;
                    BreakIterator b = BreakIterator.getWordInstance();
                    b.setText(map.get(key));

                    int offset = 80;
                    int start = 0;

                    boolean first = true;

                    while (start < map.get(key).length()) {
                        if (map.get(key).length() > character_offset) {
                            if (first) {
                                temp.append("\"");
                            }
                            if (!first) {
                                temp.append(" ");
                            }

                            first = false;
                            int end = b.following(offset);
                            String str = map.get(key).substring(start, end);
                            start = end;
                            offset += 80;
                            temp.append(str).append(" %@\n");
                        } else {
                            temp.append("\"").append(map.get(key));
                            start = map.get(key).length();
                        }
                    }

                    temp.append("\";");
                    os.write(temp.toString().getBytes(Charset.forName(CHARSET_STRING)));
                } else {
                    os.write(line.getBytes());
                }
            }
        }
        in.close();
    }
}
