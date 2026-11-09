package com.lam.httpbrowser;

import com.lam.httpbrowser.ui.BrowserFrame;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.GraphicsEnvironment;

public class BrowserFrameTest {

    @Test
    public void testGuiComponentsInitialization() {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Running in headless mode, skipping GUI render test.");
            return;
        }

        BrowserFrame frame = new BrowserFrame();
        assertNotNull(frame.getTitle());
        assertEquals("http://example.com", frame.getUrl());
        assertEquals("GET", frame.getSelectedMethod());
        
        // Test clear and setters
        frame.setStatus(200, "OK");
        frame.setResponseBody("Hello World");
        frame.setResponseHeaders("Content-Type: text/html");
        frame.setHtmlStatistics(11, 11, 1, 2, 0, 0);

        frame.dispose();
    }
}

