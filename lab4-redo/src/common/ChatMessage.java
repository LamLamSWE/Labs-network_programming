package common;

import java.io.Serializable;

public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String text;
    private final long   timestamp;

    public ChatMessage(String text) {
        this.text      = text == null ? "" : text;
        this.timestamp = System.currentTimeMillis();
    }

    public String getText()       { return text; }
    public long   getTimestamp()  { return timestamp; }

    @Override
    public String toString() { return text; }
}
