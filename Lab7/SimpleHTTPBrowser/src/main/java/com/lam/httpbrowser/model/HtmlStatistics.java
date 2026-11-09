package com.lam.httpbrowser.model;

/**
 * Data model that stores the HTML structural statistics:
 * - Total characters
 * - Total UTF-8 bytes
 * - Count of <p>, <div>, <span>, and <img> elements
 */
public class HtmlStatistics {

    private final int characterCount;
    private final int utf8ByteCount;
    private final int pCount;
    private final int divCount;
    private final int spanCount;
    private final int imgCount;

    public HtmlStatistics(int characterCount, int utf8ByteCount, int pCount, int divCount, int spanCount, int imgCount) {
        this.characterCount = characterCount;
        this.utf8ByteCount = utf8ByteCount;
        this.pCount = pCount;
        this.divCount = divCount;
        this.spanCount = spanCount;
        this.imgCount = imgCount;
    }

    public int getCharacterCount() {
        return characterCount;
    }

    public int getUtf8ByteCount() {
        return utf8ByteCount;
    }

    public int getPCount() {
        return pCount;
    }

    public int getDivCount() {
        return divCount;
    }

    public int getSpanCount() {
        return spanCount;
    }

    public int getImgCount() {
        return imgCount;
    }

    @Override
    public String toString() {
        return String.format("HTML Stats [Chars: %d, Bytes: %d, <p>: %d, <div>: %d, <span>: %d, <img>: %d]",
                characterCount, utf8ByteCount, pCount, divCount, spanCount, imgCount);
    }
}

