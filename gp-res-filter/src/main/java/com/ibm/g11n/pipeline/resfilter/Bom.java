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

public class Bom {
    public static final Bom BOM_UTF_8 = new Bom(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }, "UTF-8");
    public static final Bom BOM_UTF_16_BIG = new Bom(new byte[] { (byte) 0xFE, (byte) 0xFF }, "UTF-16, big-endian");
    public static final Bom BOM_UTF_16_LITTLE = new Bom(new byte[] { (byte) 0xFF, (byte) 0xFE },
            "UTF-16, little-endian");
    public static final Bom BOM_UTF_32_BIG = new Bom(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0xFE, (byte) 0xFF },
            "UTF-32, big-endian");
    public static final Bom BOM_UTF_32_LITTLE = new Bom(
            new byte[] { (byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x00 }, "UTF-32, little-endian");

    public static final Bom[] BOMS = new Bom[] { BOM_UTF_8, BOM_UTF_16_BIG, BOM_UTF_16_LITTLE, BOM_UTF_32_BIG,
            BOM_UTF_32_LITTLE };

    private byte[] bomBytes;
    private String name;

    public Bom(byte[] bytes, String name) {
        this.name = name;
        this.bomBytes = bytes;
    }

    public byte[] getBomBytes() {
        return bomBytes;
    }

    public String getName() {
        return name;
    }

    public boolean check(byte[] bytes) {
        if (bytes == null || bytes.length < bomBytes.length) {
            return false;
        }

        for (int i = 0; i < bomBytes.length; i++) {
            if (bomBytes[i] != bytes[i]) {
                return false;
            }
        }

        return true;
    }
}
