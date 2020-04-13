package chordlookerupper;

import java.net.URI;
import java.awt.Desktop;
import java.io.IOException;

public class Browser {
    public void openURI(URI url) throws IOException {
        if (Desktop.isDesktopSupported()) {
            var desktop = Desktop.getDesktop();
            desktop.browse(url);
        } else {
            throw new UnsupportedOperationException("Desktop is not supported on this platform.");
        }
    }
}