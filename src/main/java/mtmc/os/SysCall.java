package mtmc.os;

public enum SysCall {
    HALT(0x00),
    RINT(0x01),
    WINT(0x02),
    RSTR(0x03),
    WSTR(0x04),
    RFILE(0x05),
    WFILE(0x06),
    RND(0x07),
    SLEEP(0x08),
    FBRESET(0x09),
    FBSTAT(0x0A),
    FBSET(0x0B),
    FBLINE(0x0C),
    ERROR(0xFF);

    private final byte value;

    SysCall(int value) {
        this.value = (byte) value;
    }

    public byte getValue() {
        return value;
    }

    public static boolean isSysCall(String call) {
        try {
            valueOf(call.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static byte getValue(String call) {
        return valueOf(call.toUpperCase()).value;
    }

    public static String getString(byte syscallCode) {
        for (SysCall o : SysCall.values()) {
            if (o.getValue() == syscallCode) {
                return o.name().toLowerCase();
            }
        }
        return null;
    }
}
