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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class L2Tool extends Application {
    private Stage stage;

    public Stage getStage() {
        return stage;
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;

        FXMLLoader loader = new FXMLLoader(L2Tool.class.getResource("l2tool.fxml"));
        Parent parent = loader.load();

        Controller controller = loader.getController();
        controller.setApplication(this);

        stage.setTitle("L2Tool");
        stage.getIcons().add(new Image(L2Tool.class.getResourceAsStream("L2Tool.png")));
        stage.setScene(new Scene(parent));
        stage.show();
        Platform.runLater(() -> {
            stage.setWidth(Double.parseDouble(windowPrefs().get("width", String.valueOf(stage.getWidth()))));
            stage.setHeight(Double.parseDouble(windowPrefs().get("height", String.valueOf(stage.getHeight()))));
            if (windowPrefs().getBoolean("maximized", stage.isMaximized())) {
                stage.setMaximized(true);
            } else {
                Rectangle2D bounds = new Rectangle2D(
                        Double.parseDouble(windowPrefs().get("x", String.valueOf(stage.getX()))),
                        Double.parseDouble(windowPrefs().get("y", String.valueOf(stage.getY()))),
                        stage.getWidth(),
                        stage.getHeight());
                if (Screen.getScreens()
                        .stream()
                        .map(Screen::getVisualBounds)
                        .anyMatch(r -> r.intersects(bounds))) {
                    stage.setX(bounds.getMinX());
                    stage.setY(bounds.getMinY());
                }
            }
        });

        stage.setOnCloseRequest(windowEvent -> Platform.exit());

        Platform.runLater(() -> {
            InvalidationListener listener = observable -> {
                if (stage.isMaximized()) {
                    windowPrefs().putBoolean("maximized", true);
                } else {
                    windowPrefs().putBoolean("maximized", false);
                    windowPrefs().put("x", String.valueOf(Math.round(stage.getX())));
                    windowPrefs().put("y", String.valueOf(Math.round(stage.getY())));
                    windowPrefs().put("width", String.valueOf(Math.round(stage.getWidth())));
                    windowPrefs().put("height", String.valueOf(Math.round(stage.getHeight())));
                }
            };
            stage.xProperty().addListener(listener);
            stage.yProperty().addListener(listener);
            stage.widthProperty().addListener(listener);
            stage.heightProperty().addListener(listener);
        });
    }

    public static Preferences getPrefs() {
        return Preferences.userRoot().node("l2clientmod").node("l2tool");
    }

    private static Preferences windowPrefs() {
        return getPrefs().node("window");
    }

    public static void main(String[] args) {
        if (args.length > 0 && (args[0].equals("-export") || args[0].equals("-convert"))) {
            L2ToolCmd.main(args);
        } else {
            launch(args);
        }
    }
}
