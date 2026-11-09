package common;

public final class Protocol {

    private Protocol() {}

    public static final byte REGISTER            = 0x10;
    public static final byte LOGIN              = 0x11;
    public static final byte LOGOUT             = 0x12;
    public static final byte LIST_USERS          = 0x13;
    public static final byte USER_LIST           = 0x14;

    public static final byte CHAT_SEND          = 0x20;
    public static final byte CHAT_RECV          = 0x21;
    public static final byte SYSTEM_NOTIFY      = 0x22;

    public static final byte FILE_OFFER         = 0x30;
    public static final byte FILE_OFFER_INCOMING = 0x31;
    public static final byte FILE_ACCEPT         = 0x32;
    public static final byte FILE_REJECT         = 0x33;
    public static final byte FILE_ACCEPTED       = 0x34;
    public static final byte FILE_REJECTED       = 0x35;
    public static final byte FILE_CHUNK          = 0x36;
    public static final byte FILE_CHUNK_INCOMING = 0x37;
    public static final byte FILE_END            = 0x38;
    public static final byte FILE_COMPLETE       = 0x39;
    public static final byte FILE_ERROR          = (byte) 0x3A;

    public static final byte OK                  = (byte) 0xF0;
    public static final byte FAIL                = (byte) 0xF1;
    public static final byte ERR_USERNAME_EXISTS = (byte) 0xF2;
    public static final byte ERR_BAD_CRED        = (byte) 0xF3;
    public static final byte ERR_USER_OFFLINE    = (byte) 0xF4;
    public static final byte ERR_NOT_LOGGED_IN   = (byte) 0xF5;
    public static final byte ERR_INVALID_ARGS    = (byte) 0xF6;
}
