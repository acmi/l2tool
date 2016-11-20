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

import acmi.l2.clientmod.io.UnrealPackage;
import acmi.l2.clientmod.l2tool.img.*;
import acmi.l2.clientmod.texconv.ConvertTool;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class L2ToolCmd {
    private static File createParents(File f) throws IOException {
        File parent = f.getParentFile();
        if (!parent.exists()) {
            if (!parent.mkdirs())
                throw new IOException("Couldn't create folder: " + parent);
        }

        return f;
    }

    private static String fixPath(String path) {
        while (path.indexOf(46) != -1) {
            path = path.replace(".", File.separator);
        }

        return path;
    }

    private static void export(UnrealPackage.ExportEntry entry) {
        try {
            byte[] raw = entry.getObjectRawData();
            MipMapInfo info = MipMapInfo.getInfo(entry);
            switch (info.format) {
                case DXT1:
                case DXT3:
                case DXT5:
                    DDS.createFromData(raw, info).write(createParents(new File(fixPath(entry.getObjectFullName()) + ".dds")));
                    System.out.println(" " + info.format);
                    break;
                case RGBA8:
                    TGA.createFromData(raw, info).write(createParents(new File(fixPath(entry.getObjectFullName()) + ".tga")));
                    System.out.println(" " + info.format);
                    break;
                case P8:
                    P8.createFromData(raw, info).write(createParents(new File(fixPath(entry.getObjectFullName()) + ".bmp")));
                    System.out.println(" " + info.format);
                    break;
                case G16:
                    G16.createFromData(raw, info).write(createParents(new File(fixPath(entry.getObjectFullName()) + ".bmp")));
                    System.out.println(" " + info.format);
                    break;
                default:
                    System.out.println(" not supported");
            }
        } catch (Exception e1) {
            System.out.println(" error");
        }
    }

    public static void main(String[] args) {
        switch (args[0]) {
            case "-export": {
                if (args.length > 1) {
                    File src = new File(args[1]);
                    try (UnrealPackage up = new UnrealPackage(src, true)) {
                        if (args.length > 2) {
                            Optional<UnrealPackage.ExportEntry> entry = up.getExportTable()
                                    .stream()
                                    .filter(e -> e.getObjectFullName().equalsIgnoreCase(args[2]))
                                    .filter(e -> ConvertTool.isTexture(e.getObjectClass().getObjectFullName()))
                                    .findAny();
                            if (entry.isPresent()) {
                                System.out.print(entry.get().getObjectFullName());
                                export(entry.get());
                            } else {
                                System.err.println("Texture not found");
                            }
                        } else {
                            File out = new File(src.getName().length() > 4 ?
                                    src.getName().substring(0, src.getName().length() - 4) :
                                    src.getName()
                            );
                            if (!out.exists() || !out.isDirectory())
                                if (!out.mkdir()) {
                                    System.err.println("Couldn't create out dir");
                                    System.exit(0);
                                }

                            up.getExportTable()
                                    .stream()
                                    .filter(entry -> ConvertTool.isTexture(entry.getObjectClass().getObjectFullName()))
                                    .forEach(entry -> {
                                        System.out.print(entry.getObjectFullName());
                                        export(entry);
                                    });
                        }
                    } catch (Exception e) {
                        System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                    }
                } else {
                    System.err.println("Input utx not specified");
                }
                System.exit(0);
            }
            case "-convert": {
                if (args.length > 1) {
                    File src = new File(args[1]);
                    try (UnrealPackage up = new UnrealPackage(src, true)) {
                        File dst;
                        if (args.length > 2)
                            dst = new File(args[2]);
                        else if ((dst = new File(src.getName())).getAbsolutePath().equals(src.getAbsolutePath()))
                            dst = new File("new-" + src.getName());

                        ConvertTool.save(up, dst);

                        System.out.println("Done");
                    } catch (Exception e) {
                        System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                    }
                } else {
                    System.err.println("Input utx not specified");
                }
                System.exit(0);
            }
            default: {
                System.out.println("Commands:");
                System.out.println("\t-export utx <texture_name>");
                System.out.println("\t-convert utx <new_utx>");
                System.exit(0);
            }
        }
    }
}
