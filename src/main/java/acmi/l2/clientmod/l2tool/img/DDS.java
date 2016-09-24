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

import gr.zdimensions.jsquish.Squish;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class DDS extends Img {
    private DDS() {
    }

    public static acmi.l2.clientmod.l2tool.img.DDS createFromData(byte[] data, MipMapInfo info) throws IOException {
        acmi.l2.clientmod.l2tool.img.DDS dds = new acmi.l2.clientmod.l2tool.img.DDS();
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

    public static acmi.l2.clientmod.l2tool.img.DDS loadFromFile(File file) throws IOException {
        DDSImage image = DDSImage.read(file);

        acmi.l2.clientmod.l2tool.img.DDS dds = new acmi.l2.clientmod.l2tool.img.DDS();
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

    public static Format getFormat(int format) throws IOException {
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
