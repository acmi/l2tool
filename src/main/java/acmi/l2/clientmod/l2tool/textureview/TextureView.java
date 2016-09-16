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
package acmi.l2.clientmod.l2tool.textureview;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ResourceBundle;

public class TextureView implements Initializable {
    @FXML
    private StackPane pane;
    @FXML
    private ImageView image;
    @FXML
    private CheckBox alpha;
    @FXML
    private ComboBox<Background> background;
    @FXML
    private ComboBox<Scale> scale;

    private final ObjectProperty<BufferedImage> imageProperty = new SimpleObjectProperty<>();
    private final BooleanProperty alphaEnabledProperty = new SimpleBooleanProperty(true);

    private final ObjectProperty<Background> backgroundProperty = new SimpleObjectProperty<>(Background.CHECKERBOARD);
    private final ObjectProperty<Scale> scaleProperty = new SimpleObjectProperty<>(Scale.S1);

    public Background getBackground() {
        return backgroundProperty.get();
    }

    public ObjectProperty<Background> backgroundProperty() {
        return backgroundProperty;
    }

    public void setBackground(Background background) {
        backgroundProperty.set(background);
    }

    public Scale getScale() {
        return scaleProperty.get();
    }

    public ObjectProperty<Scale> scaleProperty() {
        return scaleProperty;
    }

    public void setScale(Scale scale) {
        scaleProperty.set(scale);
    }

    public BufferedImage getImage() {
        return imageProperty.get();
    }

    public ObjectProperty<BufferedImage> imageProperty() {
        return imageProperty;
    }

    public void setImage(BufferedImage image) {
        imageProperty.set(image);
    }

    public boolean isAlphaEnabled() {
        return alphaEnabledProperty.get();
    }

    public BooleanProperty alphaEnabledProperty() {
        return alphaEnabledProperty;
    }

    public void setAlphaEnabled(boolean value) {
        alphaEnabledProperty.set(value);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        javafx.scene.layout.Background W = new javafx.scene.layout.Background(new BackgroundFill(Color.WHITE, null, null));
        javafx.scene.layout.Background B = new javafx.scene.layout.Background(new BackgroundFill(Color.BLACK, null, null));
        javafx.scene.layout.Background G = new javafx.scene.layout.Background(new BackgroundFill(Color.GRAY, null, null));
        javafx.scene.layout.Background C = new javafx.scene.layout.Background(new BackgroundImage(new Image(getClass().getResource("bg_tr.png").toExternalForm()), null, null, null, null));
        pane.backgroundProperty().bind(Bindings.createObjectBinding(() -> {
            switch (getBackground()) {
                case WHITE:
                    return W;
                case BLACK:
                    return B;
                case GRAY:
                    return G;
                case CHECKERBOARD:
                    return C;
                default:
                    return javafx.scene.layout.Background.EMPTY;
            }
        }, backgroundProperty()));

        alpha.selectedProperty().bindBidirectional(alphaEnabledProperty);
        background.getItems().addAll(Background.values());
        background.valueProperty().bindBidirectional(backgroundProperty());
        scale.getItems().addAll(Scale.values());
        scale.valueProperty().bindBidirectional(scaleProperty());

        image.imageProperty().bind(Bindings.createObjectBinding(() -> {
            BufferedImage img = getImage();
            if (img == null)
                return null;

            BufferedImage copy = new BufferedImage(img.getWidth(), img.getHeight(),
                    alphaEnabledProperty().get() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < img.getWidth(); x++)
                for (int y = 0; y < img.getHeight(); y++)
                    copy.setRGB(x, y, img.getRGB(x, y));

            WritableImage fxImage = new WritableImage(img.getWidth(), img.getHeight());
            SwingFXUtils.toFXImage(copy, fxImage);
            return fxImage;
        }, imageProperty(), alphaEnabledProperty()));
        image.fitWidthProperty().bind(Bindings.createDoubleBinding(() -> image.getImage() == null ? 0d : image.getImage().getWidth() * getScale().scale, image.imageProperty(), scaleProperty()));
        image.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> image.getImage() == null ? 0d : image.getImage().getHeight() * getScale().scale, image.imageProperty(), scaleProperty()));
    }

    public enum Background {
        WHITE,
        GRAY,
        BLACK,
        CHECKERBOARD
    }

    public enum Scale {
        S025(0.25),
        S05(0.5),
        S1(1.0),
        S2(2.0),
        S4(4.0);

        private final double scale;

        Scale(double scale) {
            this.scale = scale;
        }

        @Override
        public String toString() {
            return "x" + scale;
        }
    }
}
