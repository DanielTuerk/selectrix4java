package io.github.danieltuerk.selectrix4java.device.serial.ffm;

public class TermiosConst {

    public static final int O_RDWR = 0x0002;
    public static final int O_NOCTTY = 0x0100;

    public static final int TCSANOW = 0;

    // Baudrates
    public static final int B9600 = 13;
    public static final int B19200 = 14;
    public static final int B38400 = 15;
    public static final int B57600 = 4097;
    public static final int B115200 = 4098;

    // Flags
    public static final int CS8 = 0x30;
    public static final int CSTOPB = 0x40;
    public static final int CREAD = 0x80;
    public static final int PARENB = 0x100;
    public static final int PARODD = 0x200;
    public static final int CLOCAL = 0x800;

    public static final int ICANON = 0x0002;
    public static final int ECHO = 0x0008;
    public static final int ISIG = 0x0001;

    public static final int VMIN = 6;
    public static final int VTIME = 5;
}
