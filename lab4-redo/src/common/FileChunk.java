package common;

import java.io.Serializable;

public class FileChunk implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String transferId;
    public final int    sequence;
    public final byte[] data;

    public FileChunk(String transferId, int sequence, byte[] data) {
        this.transferId = transferId;
        this.sequence   = sequence;
        this.data       = data;
    }
}
