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

import acmi.l2.clientmod.io.*;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class Palette {
    public int exportIndex;
    public final Color[] colors;

    public Palette(int colorCount) {
        this.colors = new Color[colorCount];
    }

    public static Palette getRGBA(UnrealPackage.ExportEntry entry) {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(entry.getObjectRawData()), null);
        if (input.readCompactInt() != entry.getUnrealPackage().nameReference("None")) {
            throw new RuntimeException("Palette with properties");
        } else {
            Palette palette = new Palette(input.readCompactInt());
            readRGBA(palette.colors, input);
            palette.exportIndex = entry.getIndex();
            return palette;
        }
    }

    public void writeToUnrealPackage(UnrealPackage up) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(10 + this.colors.length * 4);
        DataOutputStream output = new DataOutputStream(baos, null);
        output.writeCompactInt(up.nameReference("None"));
        output.writeCompactInt(this.colors.length);
        writeRGBA(this.colors, output);
        up.getExportTable().get(this.exportIndex).setObjectRawData(baos.toByteArray());
    }

    public static void readRGBA(Color[] colors, DataInput input) {
        for (int i = 0; i < colors.length; ++i) {
            int r = input.readUnsignedByte();
            int g = input.readUnsignedByte();
            int b = input.readUnsignedByte();
            int a = input.readUnsignedByte();
            colors[i] = new Color(r, g, b, a);
        }
    }

    public static void readXRGB(Color[] colors, DataInput input) {
        for (int i = 0; i < colors.length; ++i) {
            int b = input.readUnsignedByte();
            int g = input.readUnsignedByte();
            int r = input.readUnsignedByte();
            input.readUnsignedByte();
            colors[i] = new Color(r, g, b);
        }
    }

    public static void writeRGBA(Color[] colors, DataOutput output) {
        for (Color color : colors) {
            output.writeByte(color.getRed());
            output.writeByte(color.getGreen());
            output.writeByte(color.getBlue());
            output.writeByte(color.getAlpha());
        }
    }

    public static void writeARGB(Color[] colors, DataOutput output) {
        for (Color color : colors) {
            output.writeByte(color.getBlue());
            output.writeByte(color.getGreen());
            output.writeByte(color.getRed());
            output.writeByte(color.getAlpha());
        }
    }
}
