package io.github.danieltuerk.selectrix4java.device.serial.ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.MemoryLayout.PathElement.sequenceElement;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Native layout of the glibc {@code struct termios} (bits/termios.h):
 * <pre>
 * struct termios {
 *     tcflag_t c_iflag;
 *     tcflag_t c_oflag;
 *     tcflag_t c_cflag;
 *     tcflag_t c_lflag;
 *     cc_t     c_line;
 *     cc_t     c_cc[NCCS]; // NCCS = 32
 *     speed_t  c_ispeed;
 *     speed_t  c_ospeed;
 * };
 * </pre>
 * Note the {@code c_line} byte between {@code c_lflag} and {@code c_cc}, plus the
 * 3 bytes of padding before {@code c_ispeed} needed to keep it 4-byte aligned —
 * both are easy to miss but shift every {@code c_cc} index (e.g. VMIN/VTIME) if omitted.
 */
public final class Termios {

    private static final int NCCS = 32;

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("c_iflag"),
            JAVA_INT.withName("c_oflag"),
            JAVA_INT.withName("c_cflag"),
            JAVA_INT.withName("c_lflag"),
            JAVA_BYTE.withName("c_line"),
            MemoryLayout.sequenceLayout(NCCS, JAVA_BYTE).withName("c_cc"),
            MemoryLayout.paddingLayout(3),
            JAVA_INT.withName("c_ispeed"),
            JAVA_INT.withName("c_ospeed")
    ).withName("termios");

    private static final VarHandle C_IFLAG = LAYOUT.varHandle(groupElement("c_iflag"));
    private static final VarHandle C_OFLAG = LAYOUT.varHandle(groupElement("c_oflag"));
    private static final VarHandle C_CFLAG = LAYOUT.varHandle(groupElement("c_cflag"));
    private static final VarHandle C_LFLAG = LAYOUT.varHandle(groupElement("c_lflag"));
    private static final VarHandle C_CC = LAYOUT.varHandle(groupElement("c_cc"), sequenceElement());
    private static final VarHandle C_ISPEED = LAYOUT.varHandle(groupElement("c_ispeed"));
    private static final VarHandle C_OSPEED = LAYOUT.varHandle(groupElement("c_ospeed"));

    private final MemorySegment segment;

    public Termios(Arena arena) {
        this.segment = arena.allocate(LAYOUT);
    }

    public MemorySegment segment() {
        return segment;
    }

    public void cIflag(int value) {
        C_IFLAG.set(segment, 0L, value);
    }

    public void cOflag(int value) {
        C_OFLAG.set(segment, 0L, value);
    }

    public void cLflag(int value) {
        C_LFLAG.set(segment, 0L, value);
    }

    public void cCflag(int value) {
        C_CFLAG.set(segment, 0L, value);
    }

    public void cc(int index, byte value) {
        C_CC.set(segment, 0L, (long) index, value);
    }

    public void cIspeed(int value) {
        C_ISPEED.set(segment, 0L, value);
    }

    public void cOspeed(int value) {
        C_OSPEED.set(segment, 0L, value);
    }
}
