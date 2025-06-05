package mtmc.os;

public enum SysCall {
    EXIT(0x00),
    RINT(0x01),
    WINT(0x02),
    RSTR(0x03),
    WCHR(0x04),
    RCHR(0x05),
    WSTR(0x06),
    RFILE(0x07),
    WFILE(0x08),
    RND(0x09),
    SLEEP(0x0A),
    FBRESET(0x0B),
    FBSTAT(0x0C),
    FBSET(0x0D),
    FBLINE(0x0E),
    FBRECT(0x0F),
    FBFLUSH(0x10),
    JOYSTICK(0x11),
    SCOLOR(0x12),
    MEMCOPY(0x13),
    ERROR(0x0FF);

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
