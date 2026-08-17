package io.github.danieltuerk.selectrix4java.device.serial.ffm.win;

public class WinConst {

    public static final int GENERIC_READ = 0x80000000;
    public static final int GENERIC_WRITE = 0x40000000;

    public static final int OPEN_EXISTING = 3;

    public static final int FILE_ATTRIBUTE_NORMAL = 0x80;

    public static final int FILE_SHARE_READ = 0x00000001;
    public static final int FILE_SHARE_WRITE = 0x00000002;

    // Parity
    public static final byte NOPARITY = 0;
    public static final byte ODDPARITY = 1;
    public static final byte EVENPARITY = 2;

    // Stop bits
    public static final byte ONESTOPBIT = 0;
    public static final byte TWOSTOPBITS = 2;

    // Registry
    public static final long HKEY_LOCAL_MACHINE = 0x80000002L;
    public static final int KEY_READ = 0x20019;
    public static final int REG_SZ = 1;
    public static final int ERROR_NO_MORE_ITEMS = 259;
}
