package com.lam.httpbrowser;

import com.lam.httpbrowser.analyzer.HtmlAnalyzer;
import com.lam.httpbrowser.model.HtmlStatistics;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class HtmlAnalyzerTest {

    private final HtmlAnalyzer analyzer = new HtmlAnalyzer();

    @Test
    public void testElementCountingAccuracy() {
        String html = "<!DOCTYPE html><html><body>" +
                "<div>First div</div>" +
                "<div>Second div with <p>a paragraph</p> and <span>a span</span></div>" +
                "<p>Second paragraph</p>" +
                "<span>Second span</span>" +
                "<img src='image1.png' alt='pic1' />" +
                "<img src='image2.png' alt='pic2' />" +
                "</body></html>";

        HtmlStatistics stats = analyzer.analyze(html);

        assertEquals(2, stats.getPCount(), "Should find exactly 2 <p> tags");
        assertEquals(2, stats.getDivCount(), "Should find exactly 2 <div> tags");
        assertEquals(2, stats.getSpanCount(), "Should find exactly 2 <span> tags");
        assertEquals(2, stats.getImgCount(), "Should find exactly 2 <img> tags");
        assertEquals(html.length(), stats.getCharacterCount());
        assertEquals(html.getBytes(StandardCharsets.UTF_8).length, stats.getUtf8ByteCount());
    }

    @Test
    public void testTagsInsideCommentsAreIgnored() {
        String html = "<html><body>" +
                "<p>Real paragraph</p>" +
                "<!-- <p>Commented paragraph</p> <div>Commented div</div> <span>Commented span</span> <img src='x.jpg'> -->" +
                "</body></html>";

        HtmlStatistics stats = analyzer.analyze(html);

        assertEquals(1, stats.getPCount(), "Must not count <p> inside comments");
        assertEquals(0, stats.getDivCount(), "Must not count <div> inside comments");
        assertEquals(0, stats.getSpanCount(), "Must not count <span> inside comments");
        assertEquals(0, stats.getImgCount(), "Must not count <img> inside comments");
    }

    @Test
    public void testEscapedTextDoesNotTriggerTagCounts() {
        String html = "<div>Here is some code: &lt;p&gt;hello&lt;/p&gt; and &lt;img&gt;</div>";

        HtmlStatistics stats = analyzer.analyze(html);

        assertEquals(1, stats.getDivCount());
        assertEquals(0, stats.getPCount());
        assertEquals(0, stats.getImgCount());
    }

    @Test
    public void testVietnameseMultibyteCharacterMetrics() {
        String html = "<p>Lập trình mạng Java với Socket và Giao thức HTTP</p>";

        HtmlStatistics stats = analyzer.analyze(html);

        assertEquals(1, stats.getPCount());
        assertEquals(html.length(), stats.getCharacterCount());
        assertEquals(html.getBytes(StandardCharsets.UTF_8).length, stats.getUtf8ByteCount());
        assertTrue(stats.getUtf8ByteCount() > stats.getCharacterCount(),
                "UTF-8 byte count must exceed character count for Vietnamese diacritics");
    }

    @Test
    public void testNullAndEmptyHtml() {
        HtmlStatistics nullStats = analyzer.analyze(null);
        assertEquals(0, nullStats.getCharacterCount());
        assertEquals(0, nullStats.getUtf8ByteCount());
        assertEquals(0, nullStats.getPCount());

        HtmlStatistics emptyStats = analyzer.analyze("");
        assertEquals(0, emptyStats.getCharacterCount());
        assertEquals(0, emptyStats.getUtf8ByteCount());
        assertEquals(0, emptyStats.getPCount());
    }

    @Test
    public void testMalformedHtmlHandledSafely() {
        String malformedHtml = "<div><p>Unclosed paragraph <span>Unclosed span <img src='test.jpg'>";

        HtmlStatistics stats = analyzer.analyze(malformedHtml);

        assertEquals(1, stats.getDivCount());
        assertEquals(1, stats.getPCount());
        assertEquals(1, stats.getSpanCount());
        assertEquals(1, stats.getImgCount());
    }
}

