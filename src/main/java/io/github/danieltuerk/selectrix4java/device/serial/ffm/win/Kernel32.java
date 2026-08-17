package io.github.danieltuerk.selectrix4java.device.serial.ffm.win;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * FFM-based bindings for the subset of kernel32.dll needed to open, configure,
 * read from and write to a COM port as a device file.
 */
final class Kernel32 {

    static final MemorySegment INVALID_HANDLE_VALUE = MemorySegment.ofAddress(-1L);

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP = SymbolLookup.libraryLookup("kernel32", Arena.global());

    private static final StructLayout CAPTURE_STATE_LAYOUT = Linker.Option.captureStateLayout();
    private static final VarHandle LAST_ERROR_HANDLE =
            CAPTURE_STATE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("GetLastError"));
    private static final Linker.Option CAPTURE_LAST_ERROR = Linker.Option.captureCallState("GetLastError");

    private static final MethodHandle CREATE_FILE_W = downcall(
            "CreateFileW",
            FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS)
    );

    private static final MethodHandle READ_FILE = downcall(
            "ReadFile",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS)
    );

    private static final MethodHandle WRITE_FILE = downcall(
            "WriteFile",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS)
    );

    private static final MethodHandle CLOSE_HANDLE = LINKER.downcallHandle(
            LOOKUP.find("CloseHandle").orElseThrow(),
            FunctionDescriptor.of(JAVA_INT, ADDRESS)
    );

    private static final MethodHandle GET_COMM_STATE = downcall(
            "GetCommState",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS)
    );

    private static final MethodHandle SET_COMM_STATE = downcall(
            "SetCommState",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS)
    );

    private static final MethodHandle SET_COMM_TIMEOUTS = downcall(
            "SetCommTimeouts",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS)
    );

    private Kernel32() {
    }

    private static MethodHandle downcall(String symbol, FunctionDescriptor descriptor) {
        return LINKER.downcallHandle(LOOKUP.find(symbol).orElseThrow(), descriptor, CAPTURE_LAST_ERROR);
    }

    private static MemorySegment captureState(Arena arena) {
        return arena.allocate(CAPTURE_STATE_LAYOUT);
    }

    private static int lastError(MemorySegment captureState) {
        return (int) LAST_ERROR_HANDLE.get(captureState, 0L);
    }

    /**
     * Result of a call that also reports the captured GetLastError() value, since FFM does
     * not guarantee errno/GetLastError survives the transition back into Java code otherwise.
     */
    record Result<T>(T value, int lastError) {
    }

    static Result<MemorySegment> createFileW(Arena arena, String fileName, int desiredAccess, int shareMode,
                                             int creationDisposition, int flagsAndAttributes) {
        MemorySegment name = arena.allocateFrom(fileName, StandardCharsets.UTF_16LE);
        MemorySegment captureState = captureState(arena);
        try {
            MemorySegment handle = (MemorySegment) CREATE_FILE_W.invokeExact(
                    captureState, name, desiredAccess, shareMode, MemorySegment.NULL,
                    creationDisposition, flagsAndAttributes, MemorySegment.NULL);
            return new Result<>(handle, lastError(captureState));
        } catch (Throwable t) {
            throw new RuntimeException("CreateFileW native call failed", t);
        }
    }

    static Result<Integer> readFile(Arena arena, MemorySegment handle, MemorySegment buffer, int bytesToRead) {
        MemorySegment captureState = captureState(arena);
        MemorySegment bytesRead = arena.allocate(JAVA_INT);
        try {
            int ok = (int) READ_FILE.invokeExact(captureState, handle, buffer, bytesToRead, bytesRead, MemorySegment.NULL);
            int value = ok != 0 ? bytesRead.get(JAVA_INT, 0) : -1;
            return new Result<>(value, lastError(captureState));
        } catch (Throwable t) {
            throw new RuntimeException("ReadFile native call failed", t);
        }
    }

    static Result<Integer> writeFile(Arena arena, MemorySegment handle, MemorySegment buffer, int bytesToWrite) {
        MemorySegment captureState = captureState(arena);
        MemorySegment bytesWritten = arena.allocate(JAVA_INT);
        try {
            int ok = (int) WRITE_FILE.invokeExact(captureState, handle, buffer, bytesToWrite, bytesWritten, MemorySegment.NULL);
            int value = ok != 0 ? bytesWritten.get(JAVA_INT, 0) : -1;
            return new Result<>(value, lastError(captureState));
        } catch (Throwable t) {
            throw new RuntimeException("WriteFile native call failed", t);
        }
    }

    static void closeHandle(MemorySegment handle) {
        try {
            int unused = (int) CLOSE_HANDLE.invokeExact(handle);
        } catch (Throwable t) {
            throw new RuntimeException("CloseHandle native call failed", t);
        }
    }

    static Result<Boolean> getCommState(Arena arena, MemorySegment handle, MemorySegment dcb) {
        MemorySegment captureState = captureState(arena);
        try {
            int ok = (int) GET_COMM_STATE.invokeExact(captureState, handle, dcb);
            return new Result<>(ok != 0, lastError(captureState));
        } catch (Throwable t) {
            throw new RuntimeException("GetCommState native call failed", t);
        }
    }

    static Result<Boolean> setCommState(Arena arena, MemorySegment handle, MemorySegment dcb) {
        MemorySegment captureState = captureState(arena);
        try {
            int ok = (int) SET_COMM_STATE.invokeExact(captureState, handle, dcb);
            return new Result<>(ok != 0, lastError(captureState));
        } catch (Throwable t) {
            throw new RuntimeException("SetCommState native call failed", t);
        }
    }

    static Result<Boolean> setCommTimeouts(Arena arena, MemorySegment handle, MemorySegment timeouts) {
        MemorySegment captureState = captureState(arena);
        try {
            int ok = (int) SET_COMM_TIMEOUTS.invokeExact(captureState, handle, timeouts);
            return new Result<>(ok != 0, lastError(captureState));
        } catch (Throwable t) {
            throw new RuntimeException("SetCommTimeouts native call failed", t);
        }
    }
}
