package net.wbz.selectrix4java.jna.win;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public interface Kernel32 extends StdCallLibrary {

    Kernel32 INSTANCE = Native.load(
        "kernel32",
        Kernel32.class,
        W32APIOptions.UNICODE_OPTIONS
    );

    HANDLE CreateFileW(
        String lpFileName,
        int dwDesiredAccess,
        int dwShareMode,
        Pointer lpSecurityAttributes,
        int dwCreationDisposition,
        int dwFlagsAndAttributes,
        Pointer hTemplateFile
    );

    boolean ReadFile(
        HANDLE hFile,
        byte[] buffer,
        int bytesToRead,
        IntByReference bytesRead,
        Pointer overlapped
    );

    boolean WriteFile(
        HANDLE hFile,
        byte[] buffer,
        int bytesToWrite,
        IntByReference bytesWritten,
        Pointer overlapped
    );

    boolean CloseHandle(HANDLE hObject);

    boolean GetCommState(HANDLE handle, DCB dcb);

    boolean SetCommState(HANDLE handle, DCB dcb);

    boolean SetCommTimeouts(HANDLE handle, COMMTIMEOUTS timeouts);

    // 👉 DAS FEHLT BEI DIR:
    int GetLastError();
}