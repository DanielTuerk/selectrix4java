package io.github.danieltuerk.selectrix4java.device.serial.ffm.win;

import io.github.danieltuerk.selectrix4java.device.serial.ffm.SerialConfig;
import io.github.danieltuerk.selectrix4java.device.serial.ffm.SerialPortService;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public class WindowsSerialService implements SerialPortService {

    private Arena arena;
    private MemorySegment handle;
    private COMMTIMEOUTS timeouts;

    @Override
    public void open(String port, SerialConfig config) {

        // COM10+ special handling
        if (port.startsWith("COM") && port.length() > 4) {
            port = "\\\\.\\" + port;
        }

        // shared, not confined: open() and read()/write() typically run on different threads
        // (e.g. BusDataChannel opens on one thread but drives I/O from a dedicated executor thread)
        arena = Arena.ofShared();

        Kernel32.Result<MemorySegment> result = Kernel32.createFileW(
                arena,
                port,
                WinConst.GENERIC_READ | WinConst.GENERIC_WRITE,
                WinConst.FILE_SHARE_READ | WinConst.FILE_SHARE_WRITE,
                WinConst.OPEN_EXISTING,
                0
        );

        handle = result.value();

        if (handle == null || handle.equals(Kernel32.INVALID_HANDLE_VALUE)) {
            arena.close();
            throw new RuntimeException("Cannot open port: " + port + " error=" + result.lastError());
        }

        timeouts = new COMMTIMEOUTS(arena);
        configurePort(config);
    }

    private void configurePort(SerialConfig cfg) {

        DCB dcb = new DCB(arena);

        Kernel32.Result<Boolean> getResult = Kernel32.getCommState(arena, handle, dcb.segment());
        if (!getResult.value()) {
            throw new RuntimeException("GetCommState failed error=" + getResult.lastError());
        }

        dcb.baudRate(cfg.baudRate());
        dcb.byteSize((byte) cfg.dataBits());

        dcb.parity(switch (cfg.parity()) {
            case NONE -> WinConst.NOPARITY;
            case EVEN -> WinConst.EVENPARITY;
            case ODD -> WinConst.ODDPARITY;
        });

        dcb.stopBits(switch (cfg.stopBits()) {
            case 1 -> WinConst.ONESTOPBIT;
            case 2 -> WinConst.TWOSTOPBITS;
            default -> throw new IllegalArgumentException("Invalid stop bits");
        });

        Kernel32.Result<Boolean> setResult = Kernel32.setCommState(arena, handle, dcb.segment());
        if (!setResult.value()) {
            throw new RuntimeException("SetCommState failed error=" + setResult.lastError());
        }
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

            Kernel32.Result<Integer> result = Kernel32.writeFile(callArena, handle, buffer, data.length);

            if (result.value() < 0) {
                throw new RuntimeException("Write failed error=" + result.lastError());
            }
        }
    }

    @Override
    public int read(byte[] buffer, int timeoutMs) {

        configureTimeout(timeoutMs);

        try (Arena callArena = Arena.ofConfined()) {

            MemorySegment nativeBuffer = callArena.allocate(Math.max(buffer.length, 1));

            Kernel32.Result<Integer> result = Kernel32.readFile(callArena, handle, nativeBuffer, buffer.length);

            if (result.value() < 0) {
                throw new RuntimeException("Read failed error=" + result.lastError());
            }

            MemorySegment.copy(nativeBuffer, JAVA_BYTE, 0, buffer, 0, result.value());

            return result.value();
        }
    }

    private void configureTimeout(int timeoutMs) {

        if (timeoutMs <= 0) {
            // non-blocking
            timeouts.readIntervalTimeout(0xFFFFFFFF);
            timeouts.readTotalTimeoutMultiplier(0);
            timeouts.readTotalTimeoutConstant(0);
        } else {
            timeouts.readIntervalTimeout(0);
            timeouts.readTotalTimeoutMultiplier(0);
            timeouts.readTotalTimeoutConstant(timeoutMs);
        }

        Kernel32.Result<Boolean> result = Kernel32.setCommTimeouts(arena, handle, timeouts.segment());
        if (!result.value()) {
            throw new RuntimeException("SetCommTimeouts failed error=" + result.lastError());
        }
    }

    @Override
    public void close() {

        if (handle != null && !handle.equals(Kernel32.INVALID_HANDLE_VALUE)) {
            Kernel32.closeHandle(handle);
        }

        if (arena != null) {
            arena.close();
        }
    }
}
