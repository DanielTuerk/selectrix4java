package io.github.danieltuerk.selectrix4java.jna.win;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import io.github.danieltuerk.selectrix4java.jna.SerialConfig;
import io.github.danieltuerk.selectrix4java.jna.SerialPortService;

public class WindowsSerialService implements SerialPortService {

    private WinNT.HANDLE handle;

    @Override
    public void open(String port, SerialConfig config) {

        // COM10+ special handling
        if (port.startsWith("COM") && port.length() > 4) {
            port = "\\\\.\\" + port;
        }

        handle = Kernel32.INSTANCE.CreateFileW(
            port,
            WinConst.GENERIC_READ | WinConst.GENERIC_WRITE,
            WinConst.FILE_SHARE_READ | WinConst.FILE_SHARE_WRITE,
            null,
            WinConst.OPEN_EXISTING,
            0,
            null
        );

        if (handle == null || WinNT.INVALID_HANDLE_VALUE.equals(handle)) {
            int err = Kernel32.INSTANCE.GetLastError();
            throw new RuntimeException("Cannot open port: " + port + " error=" + err);
        }

        configurePort(config);
    }

    private void configurePort(SerialConfig cfg) {

        DCB dcb = new DCB();

        if (!Kernel32.INSTANCE.GetCommState(handle, dcb)) {
            int err = Kernel32.INSTANCE.GetLastError();
            throw new RuntimeException("GetCommState failed error=" + err);
        }

        dcb.BaudRate = cfg.baudRate();
        dcb.ByteSize = (byte) cfg.dataBits();

        dcb.Parity = switch (cfg.parity()) {
            case NONE -> WinConst.NOPARITY;
            case EVEN -> WinConst.EVENPARITY;
            case ODD -> WinConst.ODDPARITY;
        };

        dcb.StopBits = switch (cfg.stopBits()) {
            case 1 -> WinConst.ONESTOPBIT;
            case 2 -> WinConst.TWOSTOPBITS;
            default -> throw new IllegalArgumentException("Invalid stop bits");
        };

        if (!Kernel32.INSTANCE.SetCommState(handle, dcb)) {
            int err = Kernel32.INSTANCE.GetLastError();
            throw new RuntimeException("SetCommState failed error=" + err);
        }
    }

    @Override
    public int available() {
        byte[] buf = new byte[1024];
        return read(buf, 0);
    }

    @Override
    public void write(byte[] data) {

        IntByReference written = new IntByReference();

        boolean ok = Kernel32.INSTANCE.WriteFile(
            handle,
            data,
            data.length,
            written,
            null
        );

        if (!ok) {
            int err = Kernel32.INSTANCE.GetLastError();
            throw new RuntimeException("Write failed error=" + err);
        }
    }

    @Override
    public int read(byte[] buffer, int timeoutMs) {

        configureTimeout(timeoutMs);

        IntByReference read = new IntByReference();

        boolean ok = Kernel32.INSTANCE.ReadFile(
            handle,
            buffer,
            buffer.length,
            read,
            null
        );

        if (!ok) {
            int err = Kernel32.INSTANCE.GetLastError();
            throw new RuntimeException("Read failed error=" + err);
        }

        return read.getValue();
    }

    private void configureTimeout(int timeoutMs) {

        COMMTIMEOUTS t = new COMMTIMEOUTS();

        if (timeoutMs <= 0) {
            // non-blocking
            t.ReadIntervalTimeout = 0xFFFFFFFF;
            t.ReadTotalTimeoutMultiplier = 0;
            t.ReadTotalTimeoutConstant = 0;
        } else {
            t.ReadIntervalTimeout = 0;
            t.ReadTotalTimeoutMultiplier = 0;
            t.ReadTotalTimeoutConstant = timeoutMs;
        }

        if (!Kernel32.INSTANCE.SetCommTimeouts(handle, t)) {
            int err = Kernel32.INSTANCE.GetLastError();
            throw new RuntimeException("SetCommTimeouts failed error=" + err);
        }
    }

    @Override
    public void close() {

        if (handle != null && !WinNT.INVALID_HANDLE_VALUE.equals(handle)) {
            Kernel32.INSTANCE.CloseHandle(handle);
        }
    }
}