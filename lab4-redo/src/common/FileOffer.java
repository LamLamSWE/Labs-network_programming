package common;

import java.io.Serializable;

public class FileOffer implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String transferId;
    public final String fileName;
    public final long   fileSize;
    public final String sha256;

    public FileOffer(String transferId, String fileName, long fileSize, String sha256) {
        this.transferId = transferId;
        this.fileName   = fileName;
        this.fileSize   = fileSize;
        this.sha256     = sha256 == null ? "" : sha256;
    }

    @Override
    public String toString() {
        return fileName + " (" + fileSize + " bytes, id=" + transferId + ")";
    }
}
