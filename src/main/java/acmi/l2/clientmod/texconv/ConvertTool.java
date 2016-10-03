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
package acmi.l2.clientmod.texconv;

import acmi.l2.clientmod.io.UnrealPackage;
import acmi.l2.clientmod.l2tool.img.TextureProperties;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Function;

import static acmi.l2.clientmod.io.BufferUtil.getCompactInt;
import static acmi.l2.clientmod.io.BufferUtil.getString;
import static acmi.l2.clientmod.io.ByteUtil.compactIntToByteArray;
import static acmi.l2.clientmod.io.UnrealPackage.ObjectFlag.*;
import static java.lang.Integer.reverseBytes;
import static java.lang.Short.reverseBytes;

public class ConvertTool {
    private static final int NEW_PACKAGE_VERSION = 0x76000000;

    public static void save(UnrealPackage up, File savePath) throws IOException {
        save(up, savePath, System.out);
    }

    public static void save(UnrealPackage up, File savePath, PrintStream log) throws IOException {
        if (log == null)
            log = new PrintStream(new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                }
            });

        List<UnrealPackage.NameEntry> nameTable = new ArrayList<>(up.getNameTable());
        List<UnrealPackage.ImportEntry> importTable = new ArrayList<>(up.getImportTable());
        List<UnrealPackage.ExportEntry> exportTable = new ArrayList<>(up.getExportTable());

        int corePackageRef;
        if ((corePackageRef = up.objectReferenceByName("Core.Package", c -> c.equalsIgnoreCase("Core.Class"))) == 0) {
            boolean createCorePackage = false;
            for (UnrealPackage.ExportEntry exportEntry : exportTable) {
                String objClass = exportEntry.getFullClassName();
                if (!AS_IS.contains(objClass) &&
                        !WITH_PROPS.contains(objClass) &&
                        !TEXTURE.contains(objClass)) {
                    createCorePackage = true;
                }
            }

            if (createCorePackage) {
                Map<String, Integer> map = new LinkedHashMap<>();
                map.put("Core", UnrealPackage.ObjectFlag.getFlags(TagExp, LoadForServer, LoadForEdit, Native));
                map.put("Class", UnrealPackage.ObjectFlag.getFlags(TagExp, HighlightedName, LoadForServer, LoadForEdit, Native));
                map.put("Package", UnrealPackage.ObjectFlag.getFlags(TagExp, HighlightedName, LoadForServer, LoadForEdit, Native));
                for (Map.Entry<String, Integer> entry : map.entrySet()) {
                    UnrealPackage.NameEntry nameEntry = new UnrealPackage.NameEntry(up, nameTable.size(), entry.getKey(), entry.getValue());
                    if (!nameTable.contains(nameEntry))
                        nameTable.add(nameEntry);
                }
                Function<String, Integer> name = n -> nameTable.stream()
                        .filter(ne -> ne.getName().equalsIgnoreCase(n))
                        .findAny()
                        .map(ne -> ne.getIndex())
                        .orElseThrow(() -> new IllegalStateException(""));

                UnrealPackage.ImportEntry core = new UnrealPackage.ImportEntry(
                        up, importTable.size(),
                        name.apply("Core"), name.apply("Package"), 0, name.apply("Core"));
                int coreIndex;
                if ((coreIndex = importTable.indexOf(core)) == -1) {
                    coreIndex = importTable.size();
                    importTable.add(core);
                }
                importTable.add(new UnrealPackage.ImportEntry(
                        up, importTable.size(),
                        name.apply("Core"), name.apply("Class"), -(coreIndex + 1), name.apply("Package")));
                corePackageRef = -importTable.size();
            }
        }

        try (RandomAccessFile dest = new RandomAccessFile(savePath, "rw")) {
            Field objectPackage = UnrealPackage.Entry.class.getDeclaredField("objectPackage");
            objectPackage.setAccessible(true);
            Field objectName = UnrealPackage.Entry.class.getDeclaredField("objectName");
            objectName.setAccessible(true);
            Field classPackage = UnrealPackage.ImportEntry.class.getDeclaredField("classPackage");
            classPackage.setAccessible(true);
            Field className = UnrealPackage.ImportEntry.class.getDeclaredField("className");
            className.setAccessible(true);

            dest.setLength(0);

            dest.writeInt(reverseBytes(0x9E2A83C1));
            dest.writeInt(NEW_PACKAGE_VERSION);
            dest.writeInt(Integer.reverseBytes(up.getFlags()));
            dest.writeInt(Integer.reverseBytes(nameTable.size()));
            dest.writeInt(0);
            dest.writeInt(Integer.reverseBytes(exportTable.size()));
            dest.writeInt(0);
            dest.writeInt(Integer.reverseBytes(importTable.size()));
            dest.writeInt(0);
            dest.writeInt(reverseBytes((int) (up.getGUID().getMostSignificantBits() >> 32)));
            dest.writeShort(reverseBytes((short) (up.getGUID().getMostSignificantBits() >> 16)));
            dest.writeShort(reverseBytes((short) up.getGUID().getMostSignificantBits()));
            dest.writeLong(up.getGUID().getLeastSignificantBits());
            dest.writeInt(Integer.reverseBytes(up.getGenerations().size()));
            for (UnrealPackage.Generation generation : up.getGenerations()) {
                dest.writeInt(Integer.reverseBytes(generation.getExportCount()));
                dest.writeInt(Integer.reverseBytes(generation.getNameCount()));
            }

            int noneInd = 0;
            int nameOffset = (int) dest.getFilePointer();
            dest.seek(16);
            dest.writeInt(reverseBytes(nameOffset));
            dest.seek(nameOffset);
            for (int i = 0; i < nameTable.size(); i++) {
                UnrealPackage.NameEntry nameEntry = nameTable.get(i);
                if (!isASCII(nameEntry.getName()))
                    log.println("UTF->ASCII: " + nameEntry.getName());
                writeString(dest, nameEntry.getName());
                dest.writeInt(Integer.reverseBytes(nameEntry.getFlags()));

                if (nameEntry.getName().equals("None"))
                    noneInd = i;
            }

            int[] exportSizes = new int[exportTable.size()];
            int[] exportOffsets = new int[exportTable.size()];
            for (int i = 0; i < exportTable.size(); i++) {
                exportOffsets[i] = (int) dest.getFilePointer();
                UnrealPackage.ExportEntry exportEntry = exportTable.get(i);
                byte[] raw = exportEntry.getObjectRawData();
                byte[] data = convert(up, raw, exportEntry.getObjectClass().toString(), up.getVersion(), up.getLicense(), noneInd, exportEntry.getOffset(), exportOffsets[i]);
                exportSizes[i] = data.length;
                dest.write(data);
            }

            int importOffset = (int) dest.getFilePointer();
            dest.seek(32);
            dest.writeInt(reverseBytes(importOffset));
            dest.seek(importOffset);
            for (UnrealPackage.ImportEntry importEntry : importTable) {
                writeCompactInt(dest, classPackage.getInt(importEntry));
                writeCompactInt(dest, className.getInt(importEntry));
                dest.writeInt(Integer.reverseBytes(objectPackage.getInt(importEntry)));
                writeCompactInt(dest, objectName.getInt(importEntry));
            }

            int exportOffset = (int) dest.getFilePointer();
            dest.seek(24);
            dest.writeInt(reverseBytes(exportOffset));
            dest.seek(exportOffset);
            for (int i = 0; i < exportTable.size(); i++) {
                UnrealPackage.ExportEntry exportEntry = exportTable.get(i);
                int objClass = ref(exportEntry.getObjectClass());
                String objClassName = exportEntry.getFullClassName();
                if (!AS_IS.contains(objClassName) &&
                        !WITH_PROPS.contains(objClassName) &&
                        !TEXTURE.contains(objClassName)) {
                    objClass = corePackageRef;
                    log.println("REMOVED: " + exportEntry.toString() + "[" + exportEntry.getObjectClass().getObjectFullName() + "]");
                }
                writeCompactInt(dest, objClass);
                writeCompactInt(dest, ref(exportEntry.getObjectSuperClass()));
                dest.writeInt(Integer.reverseBytes(ref(exportEntry.getObjectPackage())));
                writeCompactInt(dest, exportEntry.getObjectName().getIndex());
                dest.writeInt(Integer.reverseBytes(exportEntry.getObjectFlags()));
                writeCompactInt(dest, exportSizes[i]);
                writeCompactInt(dest, exportOffsets[i]);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static int ref(UnrealPackage.Entry e) {
        return e == null ? 0 : e.getObjectReference();
    }

    private static void writeCompactInt(RandomAccessFile raf, int v) throws IOException {
        raf.write(compactIntToByteArray(v));
    }

    private static void writeString(RandomAccessFile raf, String s) throws IOException {
        byte[] nameBytes = s.getBytes(Charset.forName("ascii"));
        writeCompactInt(raf, nameBytes.length + 1);
        raf.write(nameBytes);
        raf.write(0);
    }

    public static boolean isTexture(String objClass) {
        return objClass != null && TEXTURE.contains(objClass);
    }

    private static final Set<String> AS_IS = new HashSet<>();
    private static final Set<String> WITH_PROPS = new HashSet<>();
    private static final Set<String> TEXTURE = new HashSet<>();

    static {
        AS_IS.add("Core.Package");
        AS_IS.add("Engine.Palette");
        AS_IS.add("Engine.GFxFlash");
        AS_IS.add("Engine.Font");

        WITH_PROPS.add("Engine.Shader");
        WITH_PROPS.add("Engine.FinalBlend");
        WITH_PROPS.add("Engine.TexOscillator");
        WITH_PROPS.add("Engine.TexOscillatorTriggered");
        WITH_PROPS.add("Engine.TexCoordSource");
        WITH_PROPS.add("Engine.TexEnvMap");
        WITH_PROPS.add("Engine.TexPanner");
        WITH_PROPS.add("Engine.TexPannerTriggered");
        WITH_PROPS.add("Engine.TexRotator");
        WITH_PROPS.add("Engine.TexScaler");
        WITH_PROPS.add("Engine.Combiner");
        WITH_PROPS.add("Engine.ColorModifier");
        WITH_PROPS.add("Engine.OpacityModifier");
        WITH_PROPS.add("Engine.GlowModifier");
        WITH_PROPS.add("Engine.UserDefinableMaterial");
        WITH_PROPS.add("Engine.MaterialSequence");
        WITH_PROPS.add("Engine.ConstantColor");
        WITH_PROPS.add("Engine.FadeColor");
        WITH_PROPS.add("Engine.VertexColor");

        TEXTURE.add("Engine.Texture");
        TEXTURE.add("Engine.ColorWheel");
        TEXTURE.add("Engine.Cubemap");
        TEXTURE.add("Engine.ColorMask");
        TEXTURE.add("Engine.MaskTexture");
        TEXTURE.add("Fire.FireTexture");
        TEXTURE.add("Fire.FluidTexture");
        TEXTURE.add("Fire.FractalTexture");
        TEXTURE.add("Fire.IceTexture");
        TEXTURE.add("Fire.WaterTexture");
        TEXTURE.add("Fire.WaveTexture");
        TEXTURE.add("Fire.WetTexture");
    }

    private static boolean isASCII(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 0x7f)
                return false;
        }
        return true;
    }

    private static byte[] convert(UnrealPackage up, byte[] src, String objClass, int version, int licensee, int noneInd, int off1, int off2) {
        if (AS_IS.contains(objClass))
            return src;

        else if (WITH_PROPS.contains(objClass) || TEXTURE.contains(objClass)) {
            ByteBuffer obj = ByteBuffer.wrap(src);
            obj.order(ByteOrder.LITTLE_ENDIAN);
            ByteBuffer res = ByteBuffer.allocate(src.length);
            res.order(ByteOrder.LITTLE_ENDIAN);
            new TextureProperties().read(up, obj);
            res.put(src, 0, obj.position());

            if (TEXTURE.contains(objClass)) {
                readUnk(obj, version, licensee);

                int mipMapCount = obj.get() & 0xff;
                res.put((byte) mipMapCount);
                for (int i = 0; i < mipMapCount; i++) {
                    int off = obj.getInt();
                    int size = getCompactInt(obj);
                    byte[] indexSize = compactIntToByteArray(size);
                    res.putInt(res.position() + off2 + indexSize.length + size + 4);
                    res.put(compactIntToByteArray(size));
                    byte[] data = new byte[size];
                    obj.get(data);
                    res.put(data);
                    obj.position(off - off1);
                    res.putInt(obj.getInt());
                    res.putInt(obj.getInt());
                    res.put(obj.get());
                    res.put(obj.get());
                }
                if ("Fire.FireTexture".equals(objClass)) {
                    int sparksCount = getCompactInt(obj);
                    res.put(compactIntToByteArray(sparksCount));
                    for (int i = 0; i < sparksCount; i++) {
                        res.put(obj.get());
                        res.put(obj.get());
                        res.put(obj.get());
                        res.put(obj.get());
                        res.put(obj.get());
                        res.put(obj.get());
                        res.put(obj.get());
                        res.put(obj.get());
                    }
                }
            }

            res.flip();
            byte[] newObj = new byte[res.limit()];
            res.get(newObj);
            return newObj;
        } else {
            return new byte[]{(byte) noneInd};
        }
    }

    public static void readUnk(ByteBuffer obj, int version, int licensee) {
        if (licensee <= 10) {
            //???
        } else if (licensee <= 28) {
            //c0-ct0
            obj.position(obj.position() + 4);
        } else if (licensee <= 32) {
            //???
        } else if (licensee <= 35) {
            //ct1-ct22
            obj.position(obj.position() + 1067);
            for (int i = 0; i < 16; i++)
                getString(obj);
            getString(obj);
            obj.position(obj.position() + 4);
        } else if (licensee == 36) {
            //ct22
            obj.position(obj.position() + 1058);
            for (int i = 0; i < 16; i++)
                getString(obj);
            getString(obj);
            obj.position(obj.position() + 4);
        } else if (licensee <= 39) {
            //Epeisodion
            if (version == 129) {
                obj.position(obj.position() + 92);
                int stringCount = getCompactInt(obj);
                for (int i = 0; i < stringCount; i++) {
                    getString(obj);
                    int addStringCount = obj.get();
                    for (int j = 0; j < addStringCount; j++)
                        getString(obj);
                }
                getString(obj);
                obj.position(obj.position() + 4);
                return;
            }

            //ct23-Lindvior
            obj.position(obj.position() + 36);
            int stringCount = getCompactInt(obj);
            for (int i = 0; i < stringCount; i++) {
                getString(obj);
                int addStringCount = obj.get();
                for (int j = 0; j < addStringCount; j++)
                    getString(obj);
            }
            getString(obj);
            obj.position(obj.position() + 4);
        } else {
            //Ertheia+
            obj.position(obj.position() + 92);
            int stringCount = getCompactInt(obj);
            for (int i = 0; i < stringCount; i++) {
                getString(obj);
                int addStringCount = obj.get();
                for (int j = 0; j < addStringCount; j++)
                    getString(obj);
            }
            getString(obj);
            obj.position(obj.position() + 4);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) {
            System.out.println("USAGE: " + ConvertTool.class.getSimpleName() + " l2_utx [ued_utx]");
            System.out.println("\tl2_utx  - input");
            System.out.println("\tued_utx - output");
            System.exit(0);
        }

        try (UnrealPackage up = new UnrealPackage(new File(args[0]), true)) {
            save(up, new File(args[1]));
        }
    }
}
