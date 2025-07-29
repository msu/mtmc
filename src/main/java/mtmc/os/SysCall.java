package mtmc.os;

public enum SysCall {
    EXIT(0x00),
    RINT(0x01),
    WINT(0x02),
    RSTR(0x03),
    WCHR(0x04),
    RCHR(0x05),
    WSTR(0x06),
    PRINTF(0x07),
    ATOI(0x08),

    RFILE(0x10),
    WFILE(0x11),
    CWD(0x12),
    CHDIR(0x13),
    DIRENT(0x14),
    DFILE(0x15),

    RND(0x20),
    SLEEP(0x21),
    TIMER(0x22),

    FBRESET(0x30),
    FBSTAT(0x31),
    FBSET(0x32),
    FBLINE(0x33),
    FBRECT(0x34),
    FBFLUSH(0x35),
    JOYSTICK(0x3A),
    SCOLOR(0x3B),

    MEMCPY(0x40),

    DRAWIMG(0x50),
    DRAWIMGSZ(0x51),
    DRAWIMGCLIP(0x52),
    
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
