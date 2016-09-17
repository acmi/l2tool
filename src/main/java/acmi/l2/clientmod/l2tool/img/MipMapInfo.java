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

import acmi.l2.clientmod.io.UnrealPackage;
import acmi.l2.clientmod.texconv.ConvertTool;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static acmi.l2.clientmod.io.BufferUtil.getCompactInt;

public class MipMapInfo {
    public String name;
    public int exportIndex;

    public Img.Format format = Img.Format.P8;
    public int width;
    public int height;
    public Palette palette;
    public int[] offsets;
    public int[] sizes;

    public static MipMapInfo getInfo(UnrealPackage.ExportEntry entry) {
        byte[] exportEntryRawData = entry.getObjectRawData();
        UnrealPackage up = entry.getUnrealPackage();

        MipMapInfo info = new MipMapInfo();
        info.name = entry.toString();
        info.exportIndex = entry.getIndex();

        ByteBuffer texture = ByteBuffer.wrap(exportEntryRawData).order(ByteOrder.LITTLE_ENDIAN);
        TextureProperties properties = new TextureProperties().read(up, texture);

        ConvertTool.readUnk(texture, up.getVersion(), up.getLicense());

        info.format = properties.getFormat();
        info.width = properties.getWidth();
        info.height = properties.getHeight();
        info.offsets = new int[getCompactInt(texture)];
        info.sizes = new int[info.offsets.length];

        UnrealPackage.Entry palette = up.objectReference(properties.getPalette());
        if (palette != null)
            info.palette = Palette.getRGBA((UnrealPackage.ExportEntry) palette);

        for (int i = 0; i < info.offsets.length; i++) {
            texture.position(texture.position() + 4);
            info.sizes[i] = getCompactInt(texture);
            info.offsets[i] = texture.position();
            texture.position(texture.position() + info.sizes[i] + 10);
        }

        return info;
    }

    @Override
    public String toString() {
        return name;
    }
}
