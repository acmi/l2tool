/*
 * Copyright (c) 2016 acmi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package acmi.l2.clientmod.l2tool.img;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public abstract class Img {
    private String name;
    private Format format;
    private BufferedImage[] mipMaps;
    private byte[][] data;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public Format getFormat() {
        return format;
    }

    protected void setFormat(Format format) {
        this.format = format;
    }

    public BufferedImage[] getMipMaps() {
        return mipMaps;
    }

    protected void setMipMaps(BufferedImage[] mipMaps) {
        this.mipMaps = mipMaps;
    }

    public byte[][] getData() {
        return data;
    }

    protected void setData(byte[][] data) {
        this.data = data;
    }

    public int getWidth() {
        return getMipMaps()[0].getWidth();
    }

    public int getHeight() {
        return getMipMaps()[0].getHeight();
    }

    public abstract void write(File file) throws IOException;

    public enum Format {
        P8,
        RGBA7,
        RGB16,
        DXT1,
        RGB8,
        RGBA8,
        NODATA,
        DXT3,
        DXT5,
        L8,
        G16,
        RRRGGGBBB
    }

    static int log2(int n) {
        return log2(n, 0);
    }

    static int log2(int n, int c) {
        return n == 1 ? c : log2(n >> 1, c + 1);
    }
}
