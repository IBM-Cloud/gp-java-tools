/*
 * Copyright IBM Corp. 2015, 2018
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
package com.ibm.g11n.pipeline.resfilter.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BomInputStream extends InputStream {
    private BufferedInputStream is;
    private Bom BOM;

    public BomInputStream(InputStream is) throws IOException {
        super();
        if (is == null) {
            throw new NullPointerException();
        }
        if (is instanceof BufferedInputStream) {
            this.is = (BufferedInputStream) is;
        } else {
            this.is = new BufferedInputStream(is);
        }

        this.skipBom();
    }

    private void skipBom() throws IOException {
        byte[] bytes = new byte[4];
        this.is.mark(4);
        this.is.read(bytes);
        BOM = null;
        for (Bom b : Bom.BOMS) {
            if (b.check(bytes)) {
                BOM = b;
                break;
            }
        }
        this.is.reset();

        if (BOM != null) {
            this.is.skip(BOM.getBomBytes().length);
        }
    }

    public Bom getBOM() {
        return BOM;
    }

    @Override
    public int read() throws IOException {
        return is.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return is.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return is.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return is.skip(n);
    }

    @Override
    public int available() throws IOException {
        return is.available();
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        is.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        is.reset();
    }

    @Override
    public boolean markSupported() {
        return is.markSupported();
    }
}
