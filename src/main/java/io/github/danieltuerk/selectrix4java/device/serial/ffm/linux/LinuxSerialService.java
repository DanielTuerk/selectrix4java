package io.github.danieltuerk.selectrix4java.device.serial.ffm.linux;

import io.github.danieltuerk.selectrix4java.device.serial.ffm.LibC;
import io.github.danieltuerk.selectrix4java.device.serial.ffm.SerialConfig;
import io.github.danieltuerk.selectrix4java.device.serial.ffm.SerialPortService;
import io.github.danieltuerk.selectrix4java.device.serial.ffm.Termios;
import io.github.danieltuerk.selectrix4java.device.serial.ffm.TermiosConst;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public class LinuxSerialService implements SerialPortService {

    private Arena arena;
    private int fd;
    private Termios termios;

    @Override
    public void open(String port, SerialConfig config) {

        // shared, not confined: open() and read()/write() typically run on different threads
        // (e.g. BusDataChannel opens on one thread but drives I/O from a dedicated executor thread)
        arena = Arena.ofShared();

        LibC.Result<Integer> openResult = LibC.open(arena, port, TermiosConst.O_RDWR | TermiosConst.O_NOCTTY);
        fd = openResult.value();

        if (fd < 0) {
            arena.close();
            throw new RuntimeException("Cannot open port: " + port + " errno=" + openResult.errno());
        }

        termios = new Termios(arena);
        configurePort(config);
    }

    private void configurePort(SerialConfig cfg) {

        LibC.Result<Integer> getResult = LibC.tcgetattr(arena, fd, termios.segment());
        if (getResult.value() != 0) {
            throw new RuntimeException("tcgetattr failed errno=" + getResult.errno());
        }

        // RAW
        termios.cIflag(0);
        termios.cOflag(0);
        termios.cLflag(0);

        int cflag = TermiosConst.CREAD | TermiosConst.CLOCAL;

        // Data bits
        cflag |= switch (cfg.dataBits()) {
            case 8 -> TermiosConst.CS8;
            default -> throw new IllegalArgumentException("Only 8 data bits supported now");
        };

        // Parity
        cflag |= switch (cfg.parity()) {
            case NONE -> 0;
            case EVEN -> TermiosConst.PARENB;
            case ODD -> TermiosConst.PARENB | TermiosConst.PARODD;
        };

        // Stop bits
        cflag |= switch (cfg.stopBits()) {
            case 1 -> 0;
            case 2 -> TermiosConst.CSTOPB;
            default -> throw new IllegalArgumentException("Invalid stop bits");
        };

        termios.cCflag(cflag);

        // Baudrate
        int baud = mapBaud(cfg.baudRate());
        termios.cIspeed(baud);
        termios.cOspeed(baud);

        // Blocking
        termios.cc(TermiosConst.VMIN, (byte) 1);
        termios.cc(TermiosConst.VTIME, (byte) 0);

        LibC.Result<Integer> setResult = LibC.tcsetattr(arena, fd, TermiosConst.TCSANOW, termios.segment());
        if (setResult.value() != 0) {
            throw new RuntimeException("tcsetattr failed errno=" + setResult.errno());
        }
    }

    private int mapBaud(int baud) {
        return switch (baud) {
            case 9600 -> TermiosConst.B9600;
            case 19200 -> TermiosConst.B19200;
            case 38400 -> TermiosConst.B38400;
            case 57600 -> TermiosConst.B57600;
            case 115200 -> TermiosConst.B115200;
            case 230400 -> TermiosConst.B230400;
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
        try (Arena callArena = Arena.ofConfined()) {

            MemorySegment buffer = callArena.allocate(Math.max(data.length, 1));
            MemorySegment.copy(data, 0, buffer, JAVA_BYTE, 0, data.length);

            LibC.Result<Long> result = LibC.write(callArena, fd, buffer, data.length);
            if (result.value() < 0) {
                throw new RuntimeException("Write failed errno=" + result.errno());
            }
        }
    }

    @Override
    public int read(byte[] buffer, int timeoutMs) {

        configureTimeout(timeoutMs);

        try (Arena callArena = Arena.ofConfined()) {

            MemorySegment nativeBuffer = callArena.allocate(Math.max(buffer.length, 1));

            LibC.Result<Long> result = LibC.read(callArena, fd, nativeBuffer, buffer.length);
            int bytesRead = (int) Math.max(result.value(), 0);

            MemorySegment.copy(nativeBuffer, JAVA_BYTE, 0, buffer, 0, bytesRead);

            return bytesRead;
        }
    }

    private void configureTimeout(int timeoutMs) {

        LibC.tcgetattr(arena, fd, termios.segment());

        if (timeoutMs <= 0) {
            // Non-blocking
            termios.cc(TermiosConst.VMIN, (byte) 0);
            termios.cc(TermiosConst.VTIME, (byte) 0);
        } else {
            // Timeout (VTIME in 100ms units!)
            termios.cc(TermiosConst.VMIN, (byte) 0);
            termios.cc(TermiosConst.VTIME, (byte) (timeoutMs / 100));
        }

        LibC.tcsetattr(arena, fd, TermiosConst.TCSANOW, termios.segment());
    }

    @Override
    public void close() {
        LibC.close(fd);
        if (arena != null) {
            arena.close();
        }
    }
}
