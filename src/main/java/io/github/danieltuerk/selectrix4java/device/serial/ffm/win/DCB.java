package io.github.danieltuerk.selectrix4java.device.serial.ffm.win;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * Native layout of the Win32 {@code DCB} struct (winbase.h), matching field-for-field
 * the layout the previous JNA {@code Structure} subclass relied on.
 */
final class DCB {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("DCBlength"),
            JAVA_INT.withName("BaudRate"),
            JAVA_INT.withName("flags"),
            JAVA_SHORT.withName("wReserved"),
            JAVA_SHORT.withName("XonLim"),
            JAVA_SHORT.withName("XoffLim"),
            JAVA_BYTE.withName("ByteSize"),
            JAVA_BYTE.withName("Parity"),
            JAVA_BYTE.withName("StopBits"),
            JAVA_BYTE.withName("XonChar"),
            JAVA_BYTE.withName("XoffChar"),
            JAVA_BYTE.withName("ErrorChar"),
            JAVA_BYTE.withName("EofChar"),
            JAVA_BYTE.withName("EvtChar"),
            JAVA_SHORT.withName("wReserved1")
    ).withName("DCB");

    private static final VarHandle DCB_LENGTH = LAYOUT.varHandle(groupElement("DCBlength"));
    private static final VarHandle BAUD_RATE = LAYOUT.varHandle(groupElement("BaudRate"));
    private static final VarHandle BYTE_SIZE = LAYOUT.varHandle(groupElement("ByteSize"));
    private static final VarHandle PARITY = LAYOUT.varHandle(groupElement("Parity"));
    private static final VarHandle STOP_BITS = LAYOUT.varHandle(groupElement("StopBits"));

    private final MemorySegment segment;

    DCB(Arena arena) {
        this.segment = arena.allocate(LAYOUT);
        DCB_LENGTH.set(segment, 0L, (int) LAYOUT.byteSize());
    }

    MemorySegment segment() {
        return segment;
    }

    void baudRate(int value) {
        BAUD_RATE.set(segment, 0L, value);
    }

    void byteSize(byte value) {
        BYTE_SIZE.set(segment, 0L, value);
    }

    void parity(byte value) {
        PARITY.set(segment, 0L, value);
    }

    void stopBits(byte value) {
        STOP_BITS.set(segment, 0L, value);
    }
}
