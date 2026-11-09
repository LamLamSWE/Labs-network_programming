package com.lam.httpbrowser;

import com.lam.httpbrowser.ui.BrowserFrame;
import javax.swing.SwingUtilities;

/**
 * Application entry point for SimpleHTTPBrowser.
 * Initializes the Swing GUI on the Event Dispatch Thread.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BrowserFrame frame = new BrowserFrame();
            frame.setVisible(true);
        });
    }
}

