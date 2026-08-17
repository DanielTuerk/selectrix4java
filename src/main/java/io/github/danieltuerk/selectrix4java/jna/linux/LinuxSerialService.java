package io.github.danieltuerk.selectrix4java.jna.linux;

import io.github.danieltuerk.selectrix4java.jna.*;

public class LinuxSerialService implements SerialPortService {

    private int fd;

    @Override
    public void open(String port, SerialConfig config) {
        fd = LibC.INSTANCE.open(port,
            TermiosConst.O_RDWR | TermiosConst.O_NOCTTY);

        if (fd < 0) {
            throw new RuntimeException("Cannot open port: " + port);
        }

        configurePort(fd, config);
    }

    private void configurePort(int fd, SerialConfig cfg) {

        Termios t = new Termios();

        if (LibC.INSTANCE.tcgetattr(fd, t) != 0) {
            throw new RuntimeException("tcgetattr failed");
        }

        // RAW
        t.c_iflag = 0;
        t.c_oflag = 0;
        t.c_lflag = 0;

        t.c_cflag =
            TermiosConst.CREAD |
                TermiosConst.CLOCAL;

        // Data bits
        t.c_cflag |= switch (cfg.dataBits()) {
            case 8 -> TermiosConst.CS8;
            default -> throw new IllegalArgumentException("Only 8 data bits supported now");
        };

        // Parity
        t.c_cflag |= switch (cfg.parity()) {
            case NONE -> 0;
            case EVEN -> TermiosConst.PARENB;
            case ODD -> TermiosConst.PARENB | TermiosConst.PARODD;
        };

        // Stop bits
        t.c_cflag |= switch (cfg.stopBits()) {
            case 1 -> 0;
            case 2 -> TermiosConst.CSTOPB;
            default -> throw new IllegalArgumentException("Invalid stop bits");
        };

        // Baudrate
        int baud = mapBaud(cfg.baudRate());
        t.c_ispeed = baud;
        t.c_ospeed = baud;

        // Blocking
        t.c_cc[TermiosConst.VMIN] = 1;
        t.c_cc[TermiosConst.VTIME] = 0;

        if (LibC.INSTANCE.tcsetattr(fd, TermiosConst.TCSANOW, t) != 0) {
            throw new RuntimeException("tcsetattr failed");
        }
    }

    private int mapBaud(int baud) {
        return switch (baud) {
            case 9600 -> TermiosConst.B9600;
            case 19200 -> TermiosConst.B19200;
            case 38400 -> TermiosConst.B38400;
            case 57600 -> TermiosConst.B57600;
            case 115200 -> TermiosConst.B115200;
            default -> throw new IllegalArgumentException("Unsupported baud: " + baud);
        };
    }
    @Override
    public int available() {
        byte[] buf = new byte[1024];
        return read(buf, 0);
    }

    @Override
    public void write(byte[] data) {
        int result = LibC.INSTANCE.write(fd, data, data.length);
        if (result < 0) {
            throw new RuntimeException("Write failed");
        }
    }

    @Override
    public int read(byte[] buffer, int timeoutMs) {

        configureTimeout(timeoutMs);

        int result = LibC.INSTANCE.read(fd, buffer, buffer.length);

        return Math.max(result, 0);
    }
    private void configureTimeout(int timeoutMs) {

        Termios t = new Termios();

        LibC.INSTANCE.tcgetattr(fd, t);

        if (timeoutMs <= 0) {
            // Non-blocking
            t.c_cc[TermiosConst.VMIN] = 0;
            t.c_cc[TermiosConst.VTIME] = 0;
        } else {
            // Timeout (VTIME in 100ms units!)
            t.c_cc[TermiosConst.VMIN] = 0;
            t.c_cc[TermiosConst.VTIME] = (byte) (timeoutMs / 100);
        }

        LibC.INSTANCE.tcsetattr(fd, TermiosConst.TCSANOW, t);
    }
    @Override
    public void close() {
        LibC.INSTANCE.close(fd);
    }
}

