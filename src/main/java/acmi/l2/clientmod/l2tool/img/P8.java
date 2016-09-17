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

import acmi.l2.clientmod.io.RandomAccessFile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;

public class P8 extends Img {
    private static final int BITS_PER_PIXEL = 8;
    public Palette palette;

    private P8(Palette palette) {
        this.palette = palette;
    }

    public static P8 createFromData(byte[] data, MipMapInfo info) {
        P8 p8 = new P8(info.palette);
        p8.setName(info.name);

        byte[] imageData = new byte[info.sizes[0]];

        for (int i = 0; i < info.sizes[0]; i += info.width)
            System.arraycopy(data, info.offsets[0] + i, imageData, imageData.length - i - info.width, info.width);

        BufferedImage image = p8.fromData(imageData, info.width, info.height, true);

        p8.setMipMaps(new BufferedImage[]{image});
        p8.setData(new byte[][]{imageData});
        return p8;
    }

    public static P8 loadFromFile(File file) throws IOException {
        try (RandomAccessFile in = new RandomAccessFile(file, true, null)) {
            int head = in.readUnsignedShort();
            if (head != 0x4d42) {
                throw new IOException("Not a bmp file " + Integer.toHexString(head));
            } else {
                in.setPosition(0x0a);
                int pixelDataPosition = in.readInt();
                int headerSize = in.readInt();
                int width;
                int height;
                int planes;
                int bitCount;
                if (headerSize == 12) {
                    width = in.readUnsignedShort();
                    height = in.readUnsignedShort();
                    planes = in.readUnsignedShort();
                    bitCount = in.readUnsignedShort();
                } else {
                    if (headerSize != 40) {
                        throw new IllegalStateException("Unknown bitmap header (size: " + headerSize + ")");
                    }

                    width = in.readInt();
                    height = in.readInt();
                    planes = in.readUnsignedShort();
                    bitCount = in.readUnsignedShort();
                }

                if (bitCount != BITS_PER_PIXEL) {
                    throw new IOException("Not a 256-color image");
                } else {
                    in.setPosition(0x0a + headerSize);
                    P8 p8 = new P8(new Palette(256));
                    p8.setFormat(Format.P8);
                    Palette.readXRGB(p8.palette.colors, in);
                    in.setPosition(pixelDataPosition);
                    byte[] data = new byte[width * height];
                    in.readFully(data);
                    BufferedImage orig = p8.fromData(data, width, height, false);
                    BufferedImage[] mipMaps = new BufferedImage[1 + log2(Math.max(width, height))];
                    mipMaps[0] = orig;
                    int w = width / 2;
                    int h = height / 2;

                    for (int ds = 1; ds < mipMaps.length; h = Math.max(h / 2, 1)) {
                        mipMaps[ds] = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, p8.getColorModel());
                        Graphics2D i = mipMaps[ds].createGraphics();
                        i.drawImage(orig, 0, 0, w, h, null);
                        i.dispose();
                        ++ds;
                        w = Math.max(w / 2, 1);
                    }

                    byte[][] mipMapsData = new byte[mipMaps.length][];

                    for (int i = 0; i < mipMaps.length; ++i) {
                        mipMapsData[i] = new byte[mipMaps[i].getWidth() * mipMaps[i].getHeight()];

                        for (int j = 0; j < mipMaps[i].getHeight(); ++j) {
                            System.arraycopy(((DataBufferByte) mipMaps[i].getRaster().getDataBuffer()).getData(), j * mipMaps[i].getWidth(), mipMapsData[i], mipMapsData[i].length - mipMaps[i].getWidth() * (j + 1), mipMaps[i].getWidth());
                        }
                    }

                    p8.setData(mipMapsData);
                    p8.setMipMaps(mipMaps);
                    return p8;
                }
            }
        }
    }

    @Override
    public void write(File file) throws IOException {
        try (acmi.l2.clientmod.io.RandomAccessFile out = new acmi.l2.clientmod.io.RandomAccessFile(file, false, null)) {
            out.writeShort(0x4d42);
            out.writeInt(0x36 + palette.colors.length * 4 + getData()[0].length);
            out.writeInt(0);
            out.writeInt(0x36 + palette.colors.length * 4);

            out.writeInt(40);
            out.writeInt(getWidth());
            out.writeInt(getHeight());
            out.writeShort(1);
            out.writeShort(BITS_PER_PIXEL);
            out.writeInt(0);
            out.writeInt(getWidth() * getHeight());
            out.writeInt(0);
            out.writeInt(0);
            out.writeInt(0);
            out.writeInt(0);
            Palette.writeARGB(palette.colors, out);
            out.writeBytes(getData()[0]);

            out.trimToPosition();
        }
    }

    private BufferedImage fromData(byte[] data, int width, int height, boolean reverseLines) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, this.getColorModel());
        if (reverseLines) {
            for (int i = 0; i < height; ++i) {
                System.arraycopy(data, i * width, ((DataBufferByte) image.getRaster().getDataBuffer()).getData(), data.length - width - i * width, width);
            }
        } else {
            System.arraycopy(data, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData(), 0, data.length);
        }

        return image;
    }

    private IndexColorModel getColorModel() {
        int size = this.palette.colors.length;
        byte[] r = new byte[size];
        byte[] g = new byte[size];
        byte[] b = new byte[size];
        byte[] a = new byte[size];

        for (int i = 0; i < size; ++i) {
            r[i] = (byte) this.palette.colors[i].getRed();
            g[i] = (byte) this.palette.colors[i].getGreen();
            b[i] = (byte) this.palette.colors[i].getBlue();
            a[i] = (byte) this.palette.colors[i].getAlpha();
        }

        return new IndexColorModel(BITS_PER_PIXEL, size, r, g, b, a);
    }
}
