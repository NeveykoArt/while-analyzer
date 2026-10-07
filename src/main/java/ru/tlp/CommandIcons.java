package ru.tlp;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;

/** Loads toolbar images from classpath resources, with a shared placeholder fallback. */
final class CommandIcons {
    private static final String DIRECTORY = "/ru/tlp/icons/";
    private static final double SIZE = 20;

    private CommandIcons() {}

    static ImageView createView(String fileName) {
        URL resource = CommandIcons.class.getResource(DIRECTORY + fileName);
        Image image = resource == null ? placeholder() : new Image(resource.toExternalForm());
        if (image.isError()) {
            image = placeholder();
        }
        ImageView view = new ImageView(image);
        view.setFitWidth(SIZE);
        view.setFitHeight(SIZE);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }

    private static Image placeholder() {
        return new Image(CommandIcons.class.getResource(DIRECTORY + "placeholder.png").toExternalForm());
    }
}
