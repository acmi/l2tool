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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class G16 extends Img {
    private G16() {
        setFormat(Format.G16);
    }

    public static acmi.l2.clientmod.l2tool.img.G16 createFromData(byte[] data, MipMapInfo info) {
        acmi.l2.clientmod.l2tool.img.G16 G16 = new acmi.l2.clientmod.l2tool.img.G16();
        G16.setName(info.name);

        byte[] imageData = new byte[info.sizes[0]];

        for (int i = 0; i < data.length - info.width * 2; i += info.width * 2)
            System.arraycopy(data, info.offsets[0] + i, imageData, imageData.length - i - info.width * 2, info.width * 2);

        ByteBuffer buffer = ByteBuffer.wrap(imageData).order(ByteOrder.LITTLE_ENDIAN);

        BufferedImage image = new BufferedImage(info.width, info.height, BufferedImage.TYPE_USHORT_GRAY);
        for (int y = info.height - 1; y >= 0; y--)
            for (int x = 0; x < info.width; x++) {
                int b = (buffer.getShort() & 0xffff) >> 8;
                image.setRGB(x, y, b | (b << 8) | (b << 16));
            }

        G16.setMipMaps(new BufferedImage[]{image});
        G16.setData(new byte[][]{imageData});
        return G16;
    }

    public static acmi.l2.clientmod.l2tool.img.G16 loadFromFile(File file) throws IOException {
        acmi.l2.clientmod.l2tool.img.G16 G16 = new acmi.l2.clientmod.l2tool.img.G16();
        G16.setFormat(Format.G16);
        G16.setName(file.getName().substring(0, file.getName().lastIndexOf('.')));

        try (acmi.l2.clientmod.io.RandomAccessFile in = new acmi.l2.clientmod.io.RandomAccessFile(file, true, null)) {
            if (in.readUnsignedShort() != 0x4d42)
                throw new IOException("Not a bmp file");

            in.setPosition(0x0a);
            int pixelDataPosition = in.readInt();

            int width;
            int height;
            int planes;
            int bitCount;

            if (in.readInt() < 40) {
                width = in.readUnsignedShort();
                height = in.readUnsignedShort();
                planes = in.readUnsignedShort();
                bitCount = in.readUnsignedShort();
            } else {
                width = in.readInt();
                height = in.readInt();
                planes = in.readUnsignedShort();
                bitCount = in.readUnsignedShort();
            }

            in.setPosition(pixelDataPosition);

            int byteCount = bitCount / 8;

            byte[] imageData = new byte[width * height * byteCount];
            for (int i = 0; i <= imageData.length - width * byteCount; i += width * byteCount)
                in.readFully(imageData, imageData.length - i - width * byteCount, width * byteCount);

            switch (byteCount) {
                case 2:
                    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);

                    ByteBuffer buffer = ByteBuffer.wrap(imageData).order(ByteOrder.LITTLE_ENDIAN);

                    for (int y = height - 1; y >= 0; y--)
                        for (int x = 0; x < width; x++) {
                            int b = (buffer.getShort() & 0xffff) >> 8;
                            image.setRGB(x, y, b | (b << 8) | (b << 16));
                        }


                    G16.setMipMaps(new BufferedImage[]{image});
                    G16.setData(new byte[][]{imageData});
                    break;
                default:
                    throw new IOException("bitCount " + bitCount + " is not supported");
            }
        }

        return G16;
    }

    @Override
    public void write(File file) throws IOException {
        try (acmi.l2.clientmod.io.RandomAccessFile out = new acmi.l2.clientmod.io.RandomAccessFile(file, false, null)) {
            out.writeShort(0x4d42);
            out.writeInt(0x36 + getData()[0].length);
            out.writeInt(0);
            out.writeInt(0x36);

            out.writeInt(40);
            out.writeInt(getWidth());
            out.writeInt(getHeight());
            out.writeShort(1);
            out.writeShort(16);
            out.writeInt(0);
            out.writeInt(getWidth() * getHeight() * 2);
            out.writeInt(0);
            out.writeInt(0);
            out.writeInt(0);
            out.writeInt(0);

            out.writeBytes(getData()[0]);

            out.trimToPosition();
        }
    }
}
