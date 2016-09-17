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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class TGA extends Img {
    private static final int GL_BGRA = 0x80E1;

    private TGA() {
        setFormat(Format.RGBA8);
    }

    public static acmi.l2.clientmod.l2tool.img.TGA createFromData(byte[] data, MipMapInfo info) {
        acmi.l2.clientmod.l2tool.img.TGA tga = new acmi.l2.clientmod.l2tool.img.TGA();
        tga.setName(info.name);
        BufferedImage[] mipMaps = new BufferedImage[info.offsets.length];
        byte[][] ds = new byte[info.offsets.length][];
        for (int i = 0; i < info.offsets.length; i++) {
            int width = Math.max(info.width / (1 << i), 1);
            int height = Math.max(info.height / (1 << i), 1);

            ds[i] = Arrays.copyOfRange(data, info.offsets[i], info.offsets[i] + info.sizes[i]);
            ByteBuffer imageBuffer = ByteBuffer.wrap(ds[i]);
            imageBuffer.order(ByteOrder.LITTLE_ENDIAN);
            BufferedImage orig = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++) {
                    orig.setRGB(x, y, imageBuffer.getInt());
                }
            mipMaps[i] = orig;
        }
        tga.setMipMaps(mipMaps);
        tga.setData(ds);
        return tga;
    }

    public static acmi.l2.clientmod.l2tool.img.TGA loadFromFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            TGAImage image = TGAImage.read(fis);
            if (image.getGLFormat() != GL_BGRA)
                throw new IOException("Not GL_BGRA format");

            acmi.l2.clientmod.l2tool.img.TGA tga = new acmi.l2.clientmod.l2tool.img.TGA();
            tga.setName(file.getName().substring(0, file.getName().lastIndexOf('.')));

            ByteBuffer imageBuffer = image.getData();
            imageBuffer.order(ByteOrder.LITTLE_ENDIAN);
            BufferedImage orig = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < image.getHeight(); y++)
                for (int x = 0; x < image.getWidth(); x++) {
                    orig.setRGB(x, y, imageBuffer.getInt());
                }

            BufferedImage[] mipMaps = new BufferedImage[1 + log2(Math.max(image.getWidth(), image.getHeight()))];
            mipMaps[0] = orig;

            int w = image.getWidth() / 2;
            int h = image.getHeight() / 2;
            for (int i = 1; i < mipMaps.length; i++, w = Math.max(w / 2, 1), h = Math.max(h / 2, 1)) {
                mipMaps[i] = new BufferedImage(w, h, orig.getType());
                Graphics2D g = mipMaps[i].createGraphics();
                g.drawImage(orig, 0, 0, w, h, null);
                g.dispose();
            }

            byte[][] ds = new byte[mipMaps.length][];
            for (int i = 0; i < mipMaps.length; i++) {
                ds[i] = new byte[mipMaps[i].getWidth() * mipMaps[i].getHeight() * 4];
                ByteBuffer buffer = ByteBuffer.wrap(ds[i]).order(ByteOrder.LITTLE_ENDIAN);
                for (int y = mipMaps[i].getHeight() - 1; y >= 0; y--)
                    for (int x = 0; x < mipMaps[i].getWidth(); x++) {
                        buffer.putInt(mipMaps[i].getRGB(x, y));
                    }
            }

            tga.setMipMaps(mipMaps);
            tga.setData(ds);
            return tga;
        }
    }

    @Override
    public void write(File file) throws IOException {
        TGAImage.createFromData(getWidth(), getHeight(), true, true, ByteBuffer.wrap(getData()[0])).write(file);
    }
}
