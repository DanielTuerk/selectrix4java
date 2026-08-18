package io.github.danieltuerk.selectrix4java.device.serial.ffm.linux;

import io.github.danieltuerk.selectrix4java.device.serial.ffm.LibC;
import io.github.danieltuerk.selectrix4java.device.serial.ffm.SerialConfig;
import io.github.danieltuerk.selectrix4java.device.serial.ffm.SerialPortService;
import io.github.danieltuerk.selectrix4java.device.serial.ffm.Termios;
import io.github.danieltuerk.selectrix4java.device.serial.ffm.TermiosConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public class LinuxSerialService implements SerialPortService {

    private static final Logger log = LoggerFactory.getLogger(LinuxSerialService.class);

    private Arena arena;
    private int fd;
    private Termios termios;
    private String port;

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

        this.port = port;
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

        // Baudrate: on Linux the kernel reads the rate from the CBAUD bits packed into
        // c_cflag (classic TCSETS path) - c_ispeed/c_ospeed alone are not enough.
        int baud = mapBaud(cfg.baudRate());
        cflag |= baud;
        termios.cCflag(cflag);
        termios.cIspeed(baud);
        termios.cOspeed(baud);

        // Blocking
        termios.cc(TermiosConst.VMIN, (byte) 1);
        termios.cc(TermiosConst.VTIME, (byte) 0);

        LibC.Result<Integer> setResult = LibC.tcsetattr(arena, fd, TermiosConst.TCSANOW, termios.segment());
        if (setResult.value() != 0) {
            throw new RuntimeException("tcsetattr failed errno=" + setResult.errno());
        }

        log.info("serial port {} configured: {} baud, {} data bits, parity {}, {} stop bits",
            port, cfg.baudRate(), cfg.dataBits(), cfg.parity(), cfg.stopBits());
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

        // With VMIN=0 a single native read() returns as soon as *any* bytes are available,
        // not once the requested length is filled (unlike Windows' ReadFile, which blocks
        // internally until ReadTotalTimeoutConstant elapses or the buffer is full). USB-serial
        // adapters commonly flush in several small chunks, so loop until the buffer is full or
        // the overall timeout budget runs out.
        long deadlineNanos = timeoutMs > 0 ? System.nanoTime() + timeoutMs * 1_000_000L : 0;
        int totalRead = 0;

        while (totalRead < buffer.length) {
            int remainingMs = timeoutMs;
            if (timeoutMs > 0) {
                remainingMs = (int) ((deadlineNanos - System.nanoTime()) / 1_000_000L);
                if (remainingMs <= 0) {
                    break;
                }
            }

            configureTimeout(remainingMs);

            try (Arena callArena = Arena.ofConfined()) {
                int toRead = buffer.length - totalRead;
                MemorySegment nativeBuffer = callArena.allocate(toRead);

                LibC.Result<Long> result = LibC.read(callArena, fd, nativeBuffer, toRead);
                int bytesRead = (int) Math.max(result.value(), 0);
                if (bytesRead <= 0) {
                    break;
                }

                MemorySegment.copy(nativeBuffer, JAVA_BYTE, 0, buffer, totalRead, bytesRead);
                totalRead += bytesRead;
            }

            if (timeoutMs <= 0) {
                // non-blocking poll: single attempt only
                break;
            }
        }

        return totalRead;
    }

    private void configureTimeout(int timeoutMs) {

        LibC.tcgetattr(arena, fd, termios.segment());

        if (timeoutMs <= 0) {
            // Non-blocking
            termios.cc(TermiosConst.VMIN, (byte) 0);
            termios.cc(TermiosConst.VTIME, (byte) 0);
        } else {
            // Timeout (VTIME in 100ms units!). Round up so a remaining budget under 100ms
            // doesn't collapse to VTIME=0, which would mean "block forever" instead.
            int vtimeUnits = Math.clamp((timeoutMs + 99) / 100, 1, 255);
            termios.cc(TermiosConst.VMIN, (byte) 0);
            termios.cc(TermiosConst.VTIME, (byte) vtimeUnits);
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
