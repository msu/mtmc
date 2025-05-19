package mtmc.asm;

import mtmc.emulator.MonTanaMiniComputer;

class FrameBufferLocation implements HasLocation {
    public static final String FRAME_BUFFER = "FRAME_BUFFER";

    @Override
    public int getLocation() {
        return MonTanaMiniComputer.FRAME_BUFF_START;
    }

    @Override
    public int getSizeInBytes() {
        return 1024;
    }
}
