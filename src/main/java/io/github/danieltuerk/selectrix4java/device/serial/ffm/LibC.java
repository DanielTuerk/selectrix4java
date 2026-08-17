package io.github.danieltuerk.selectrix4java.device.serial.ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * FFM-based bindings for the subset of the POSIX C library needed to open, configure,
 * read from and write to a serial device file.
 */
public final class LibC {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP =
            Linker.nativeLinker().defaultLookup().or(SymbolLookup.libraryLookup("c", Arena.global()));

    private static final StructLayout CAPTURE_STATE_LAYOUT = Linker.Option.captureStateLayout();
    private static final VarHandle ERRNO_HANDLE =
            CAPTURE_STATE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("errno"));
    private static final Linker.Option CAPTURE_ERRNO = Linker.Option.captureCallState("errno");

    private static final MethodHandle OPEN = downcall(
            "open", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

    private static final MethodHandle READ = downcall(
            "read", FunctionDescriptor.of(JAVA_LONG, JAVA_INT, ADDRESS, JAVA_LONG));

    private static final MethodHandle WRITE = downcall(
            "write", FunctionDescriptor.of(JAVA_LONG, JAVA_INT, ADDRESS, JAVA_LONG));

    private static final MethodHandle CLOSE = LINKER.downcallHandle(
            LOOKUP.findOrThrow("close"), FunctionDescriptor.of(JAVA_INT, JAVA_INT));

    private static final MethodHandle TCGETATTR = downcall(
            "tcgetattr", FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS));

    private static final MethodHandle TCSETATTR = downcall(
            "tcsetattr", FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS));

    private LibC() {
    }

    private static MethodHandle downcall(String symbol, FunctionDescriptor descriptor) {
        return LINKER.downcallHandle(LOOKUP.findOrThrow(symbol), descriptor, CAPTURE_ERRNO);
    }

    private static MemorySegment captureState(Arena arena) {
        return arena.allocate(CAPTURE_STATE_LAYOUT);
    }

    private static int errno(MemorySegment captureState) {
        return (int) ERRNO_HANDLE.get(captureState, 0L);
    }

    /**
     * Result of a call that also reports the captured errno value, since FFM does not
     * guarantee errno survives the transition back into Java code otherwise.
     */
    public record Result<T>(T value, int errno) {
    }

    public static Result<Integer> open(Arena arena, String path, int flags) {
        MemorySegment pathSeg = arena.allocateFrom(path);
        MemorySegment captureState = captureState(arena);
        try {
            int fd = (int) OPEN.invokeExact(captureState, pathSeg, flags);
            return new Result<>(fd, errno(captureState));
        } catch (Throwable t) {
            throw new RuntimeException("open native call failed", t);
        }
    }

    public static Result<Long> read(Arena arena, int fd, MemorySegment buffer, long count) {
        MemorySegment captureState = captureState(arena);
        try {
            long result = (long) READ.invokeExact(captureState, fd, buffer, count);
            return new Result<>(result, errno(captureState));
        } catch (Throwable t) {
            throw new RuntimeException("read native call failed", t);
        }
    }

    public static Result<Long> write(Arena arena, int fd, MemorySegment buffer, long count) {
        MemorySegment captureState = captureState(arena);
        try {
            long result = (long) WRITE.invokeExact(captureState, fd, buffer, count);
            return new Result<>(result, errno(captureState));
        } catch (Throwable t) {
            throw new RuntimeException("write native call failed", t);
        }
    }

    public static void close(int fd) {
        try {
            int unused = (int) CLOSE.invokeExact(fd);
        } catch (Throwable t) {
            throw new RuntimeException("close native call failed", t);
        }
    }

    public static Result<Integer> tcgetattr(Arena arena, int fd, MemorySegment termios) {
        MemorySegment captureState = captureState(arena);
        try {
            int result = (int) TCGETATTR.invokeExact(captureState, fd, termios);
            return new Result<>(result, errno(captureState));
        } catch (Throwable t) {
            throw new RuntimeException("tcgetattr native call failed", t);
        }
    }

    public static Result<Integer> tcsetattr(Arena arena, int fd, int optionalActions, MemorySegment termios) {
        MemorySegment captureState = captureState(arena);
        try {
            int result = (int) TCSETATTR.invokeExact(captureState, fd, optionalActions, termios);
            return new Result<>(result, errno(captureState));
        } catch (Throwable t) {
            throw new RuntimeException("tcsetattr native call failed", t);
        }
    }
}
