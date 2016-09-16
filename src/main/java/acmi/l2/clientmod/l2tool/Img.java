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
package acmi.l2.clientmod.l2tool;

import acmi.l2.clientmod.l2tool.util.DDSImage;
import acmi.l2.clientmod.l2tool.util.MipMapInfo;
import acmi.l2.clientmod.l2tool.util.TGAImage;
import gr.zdimensions.jsquish.Squish;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public abstract class Img {
    private String name;
    private String fileExtension;
    private Format format;
    private BufferedImage[] mipMaps;
    private byte[][] data;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    protected void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
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

    public static class DDS extends Img {
        private DDS() {
            setFileExtension("dds");
        }

        public static DDS createFromData(byte[] data, MipMapInfo info) throws IOException {
            DDS dds = new DDS();
            dds.setFormat(info.format);
            dds.setName(info.name);
            dds.setMipMaps(new BufferedImage[info.offsets.length]);
            dds.setData(new byte[info.offsets.length][]);
            for (int i = 0; i < info.offsets.length; i++) {
                int width = Math.max(info.width / (1 << i), 1);
                int height = Math.max(info.height / (1 << i), 1);

                byte[] compressed = Arrays.copyOfRange(data, info.offsets[i], info.offsets[i] + info.sizes[i]);
                dds.getData()[i] = compressed;
                byte[] decompressed = decompress(compressed, width, height, dds.getFormat());
                BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
                bi.getRaster().setDataElements(0, 0, width, height, decompressed);
                dds.getMipMaps()[i] = bi;
            }
            return dds;
        }

        public static DDS loadFromFile(File file) throws IOException {
            DDSImage image = DDSImage.read(file);

            DDS dds = new DDS();
            dds.setFormat(getFormat(image.getCompressionFormat()));
            dds.setName(file.getName().substring(0, file.getName().lastIndexOf('.')));
            dds.setMipMaps(new BufferedImage[image.getAllMipMaps().length]);
            dds.setData(new byte[image.getAllMipMaps().length][]);
            for (int i = 0; i < image.getAllMipMaps().length; i++) {
                DDSImage.ImageInfo info = image.getMipMap(i);

                byte[] compressed = new byte[info.getData().limit()];
                info.getData().get(compressed);
                dds.getData()[i] = compressed;

                byte[] decompressed = decompress(compressed, info.getWidth(), info.getHeight(), dds.getFormat());

                BufferedImage bi = new BufferedImage(info.getWidth(), info.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
                bi.getRaster().setDataElements(0, 0, info.getWidth(), info.getHeight(), decompressed);
                dds.getMipMaps()[i] = bi;
            }
            return dds;
        }

        private static byte[] decompress(byte[] compressed, int width, int height, Format format) throws IOException {
            Squish.CompressionType compressionType;
            try {
                compressionType = Squish.CompressionType.valueOf(format.toString());
            } catch (IllegalArgumentException iae) {
                throw new IOException("Format " + format + " is not supported");
            }
            return Squish.decompressImage(null, width, height, compressed, compressionType);
        }

        private static Format getFormat(int format) throws IOException {
            switch (format) {
                case 0x31545844:
                    return Format.DXT1;
                case 0x33545844:
                    return Format.DXT3;
                case 0x35545844:
                    return Format.DXT5;
                default:
                    throw new IOException("Unsupported format");
            }
        }

        @Override
        public void write(File file) throws IOException {
            ByteBuffer[] buffers = new ByteBuffer[getData().length];
            for (int i = 0; i < getData().length; i++)
                buffers[i] = ByteBuffer.wrap(getData()[i]);
            DDSImage image = DDSImage.createFromData(getFormat(getFormat()), getWidth(), getHeight(), buffers);
            image.write(file);
        }

        private static int getFormat(Format format) throws IOException {
            switch (format) {
                case DXT1:
                    return 0x31545844;
                case DXT3:
                    return 0x33545844;
                case DXT5:
                    return 0x35545844;
                default:
                    throw new IOException("Unsupported format");
            }
        }
    }

    public static class TGA extends Img {
        public static final int GL_BGRA = 0x80E1;

        private TGA() {
            setFileExtension("tga");
            setFormat(Format.RGBA8);
        }

        public static TGA createFromData(byte[] data, MipMapInfo info) {
            TGA tga = new TGA();
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

        public static TGA loadFromFile(File file) throws IOException {
            try (FileInputStream fis = new FileInputStream(file)) {
                TGAImage image = TGAImage.read(fis);
                if (image.getGLFormat() != GL_BGRA)
                    throw new IOException("Not GL_BGRA format");

                TGA tga = new TGA();
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

    public static class G16 extends Img {
        private G16() {
            setFileExtension("bmp");
            setFormat(Format.G16);
        }

        public static G16 createFromData(byte[] data, MipMapInfo info) {
            G16 G16 = new G16();
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

        public static G16 loadFromFile(File file) throws IOException {
            G16 G16 = new G16();
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

    public static class P8 extends Img {
        public static P8 createFromData(byte[] data, MipMapInfo info) {
            P8 p8 = new P8();
            p8.setName(info.name);

            byte[] imageData = new byte[info.sizes[0]];

            for (int i = 0; i < info.sizes[0]; i += info.width)
                System.arraycopy(data, info.offsets[0] + i, imageData, imageData.length - i - info.width, info.width);

            ByteBuffer buffer = ByteBuffer.wrap(imageData);

            BufferedImage image = new BufferedImage(info.width, info.height, BufferedImage.TYPE_INT_ARGB);
            for (int y = info.height - 1; y >= 0; y--)
                for (int x = 0; x < info.width; x++) {
                    image.setRGB(x, y, info.palette[buffer.get() & 0xff].getRGB());
                }

            p8.setMipMaps(new BufferedImage[]{image});
            p8.setData(new byte[][]{imageData});
            return p8;
        }

        @Override
        public void write(File file) throws IOException {
            throw new UnsupportedOperationException("Not implemented");
        }
    }

    private static int log2(int n) {
        return log2(n, 0);
    }

    private static int log2(int n, int c) {
        return n == 1 ? c : log2(n >> 1, c + 1);
    }
}
