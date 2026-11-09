package common;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private final byte   type;
    private final String from;
    private final String to;
    private final Object payload;

    public Message(byte type, String from, String to, Object payload) {
        this.type    = type;
        this.from    = from == null ? "" : from;
        this.to      = to   == null ? "" : to;
        this.payload = payload;
    }

    public byte   getType()    { return type; }
    public String getFrom()    { return from; }
    public String getTo()      { return to; }
    public Object getPayload() { return payload; }

    @Override
    public String toString() {
        return "Message{type=0x" + Integer.toHexString(type & 0xff)
                + ", from='" + from + "', to='" + to + "'}";
    }
}
