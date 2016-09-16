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
import acmi.l2.clientmod.l2tool.util.MipMapInfo;
import acmi.l2.clientmod.texconv.ConvertTool;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.prefs.Preferences;

public class L2Tool extends Application {
    private Stage stage;

    public Stage getStage() {
        return stage;
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parameters parameters = getParameters();
        List<String> raw = parameters.getRaw();
        if (!raw.isEmpty()) {
            cmd(raw.toArray(new String[raw.size()]));
        }

        this.stage = stage;

        FXMLLoader loader = new FXMLLoader(L2Tool.class.getResource("l2tool.fxml"));
        Parent parent = loader.load();

        Controller controller = loader.getController();
        controller.setApplication(this);

        stage.setTitle("L2Tool");
        stage.getIcons().add(new Image(L2Tool.class.getResourceAsStream("L2Tool.png")));
        stage.setScene(new Scene(parent));
        stage.show();

        stage.setOnCloseRequest(windowEvent -> Platform.exit());
    }

    private static void cmd(String[] args) {
        switch (args[0]) {
            case "-export": {
                if (args.length > 1) {
                    File src = new File(args[1]);
                    try (UnrealPackage up = new UnrealPackage(src, true)) {
                        if (args.length > 2) {
                            UnrealPackage.ExportEntry entry = null;
                            for (UnrealPackage.ExportEntry e : up.getExportTable())
                                if (e.getObjectFullName().equalsIgnoreCase(args[2]) &&
                                        ConvertTool.isTexture(e.getObjectClass().getObjectFullName()))
                                    entry = e;
                            if (entry != null) {
                                System.out.print(entry.getObjectFullName());
                                try {
                                    byte[] raw = entry.getObjectRawData();
                                    MipMapInfo info = MipMapInfo.getInfo(entry);
                                    switch (info.format) {
                                        case DXT1:
                                        case DXT3:
                                        case DXT5:
                                            Img.DDS.createFromData(raw, info).write(new File(entry.getObjectFullName() + ".dds"));
                                            System.out.println(" " + info.format);
                                            break;
                                        case RGBA8:
                                            Img.TGA.createFromData(raw, info).write(new File(entry.getObjectFullName() + ".tgs"));
                                            System.out.println(" " + info.format);
                                            break;
                                        default:
                                            System.out.println(" not supported");
                                    }
                                } catch (Exception e1) {
                                    System.out.println(" error");
                                }
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

                            for (UnrealPackage.ExportEntry e : up.getExportTable())
                                if (ConvertTool.isTexture(e.getObjectClass().getObjectFullName())) {
                                    System.out.print(e.getObjectFullName());
                                    try {
                                        byte[] raw = e.getObjectRawData();
                                        MipMapInfo info = MipMapInfo.getInfo(e);
                                        switch (info.format) {
                                            case DXT1:
                                            case DXT3:
                                            case DXT5:
                                                Img.DDS.createFromData(raw, info).write(new File(out, e.getObjectFullName() + ".dds"));
                                                System.out.println(" " + info.format);
                                                break;
                                            case RGBA8:
                                                Img.TGA.createFromData(raw, info).write(new File(out, e.getObjectFullName() + ".tga"));
                                                System.out.println(" " + info.format);
                                                break;
                                            default:
                                                System.out.println(" not supported");
                                        }
                                    } catch (Exception e1) {
                                        System.out.println(" error");
                                    }
                                }
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

    public static Preferences getPrefs() {
        return Preferences.userRoot().node("l2tool");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
