package io.github.danieltuerk.selectrix4java.device.serial.ffm.win;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Native layout of the Win32 {@code COMMTIMEOUTS} struct (winbase.h).
 */
final class COMMTIMEOUTS {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("ReadIntervalTimeout"),
            JAVA_INT.withName("ReadTotalTimeoutMultiplier"),
            JAVA_INT.withName("ReadTotalTimeoutConstant"),
            JAVA_INT.withName("WriteTotalTimeoutMultiplier"),
            JAVA_INT.withName("WriteTotalTimeoutConstant")
    ).withName("COMMTIMEOUTS");

    private static final VarHandle READ_INTERVAL_TIMEOUT = LAYOUT.varHandle(groupElement("ReadIntervalTimeout"));
    private static final VarHandle READ_TOTAL_TIMEOUT_MULTIPLIER = LAYOUT.varHandle(groupElement("ReadTotalTimeoutMultiplier"));
    private static final VarHandle READ_TOTAL_TIMEOUT_CONSTANT = LAYOUT.varHandle(groupElement("ReadTotalTimeoutConstant"));

    private final MemorySegment segment;

    COMMTIMEOUTS(Arena arena) {
        this.segment = arena.allocate(LAYOUT);
    }

    MemorySegment segment() {
        return segment;
    }

    void readIntervalTimeout(int value) {
        READ_INTERVAL_TIMEOUT.set(segment, 0L, value);
    }

    void readTotalTimeoutMultiplier(int value) {
        READ_TOTAL_TIMEOUT_MULTIPLIER.set(segment, 0L, value);
    }

    void readTotalTimeoutConstant(int value) {
        READ_TOTAL_TIMEOUT_CONSTANT.set(segment, 0L, value);
    }
}
