package com.lam.httpbrowser.analyzer;

import com.lam.httpbrowser.model.HtmlStatistics;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.nio.charset.StandardCharsets;

/**
 * Analyzes HTML content using Jsoup.
 *
 * Computes:
 * - Total character count
 * - Total UTF-8 byte count
 * - Count of specific DOM elements: <p>, <div>, <span>, <img>
 *
 * Uses DOM-based selection to avoid false positives inside comments or plain text.
 */
public class HtmlAnalyzer {

    /**
     * Parses and analyzes the provided HTML string.
     *
     * @param html the HTML content to analyze
     * @return HtmlStatistics containing counts and size metrics
     */
    public HtmlStatistics analyze(String html) {
        if (html == null || html.isEmpty()) {
            return new HtmlStatistics(0, 0, 0, 0, 0, 0);
        }

        // Calculate size metrics
        int charCount = html.length();
        int byteCount = html.getBytes(StandardCharsets.UTF_8).length;

        try {
            // Parse HTML DOM with Jsoup
            Document doc = Jsoup.parse(html);

            // DOM-based element selection
            int pCount = doc.select("p").size();
            int divCount = doc.select("div").size();
            int spanCount = doc.select("span").size();
            int imgCount = doc.select("img").size();

            return new HtmlStatistics(charCount, byteCount, pCount, divCount, spanCount, imgCount);
        } catch (Exception e) {
            // In case of any unexpected parser error, return size metrics with zero tag counts
            return new HtmlStatistics(charCount, byteCount, 0, 0, 0, 0);
        }
    }
}

