package io.github.danieltuerk.selectrix4java.device.serial.ffm.win;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * FFM-based bindings for the subset of advapi32.dll needed to enumerate the
 * registry values under {@code HKEY_LOCAL_MACHINE\HARDWARE\DEVICEMAP\SERIALCOMM}.
 */
final class Advapi32 {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP = SymbolLookup.libraryLookup("advapi32", Arena.global());

    private static final MethodHandle REG_OPEN_KEY_EX_W = LINKER.downcallHandle(
            LOOKUP.find("RegOpenKeyExW").orElseThrow(),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS)
    );

    private static final MethodHandle REG_ENUM_VALUE_W = LINKER.downcallHandle(
            LOOKUP.find("RegEnumValueW").orElseThrow(),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS)
    );

    private static final MethodHandle REG_CLOSE_KEY = LINKER.downcallHandle(
            LOOKUP.find("RegCloseKey").orElseThrow(),
            FunctionDescriptor.of(JAVA_INT, ADDRESS)
    );

    private Advapi32() {
    }

    record OpenResult(int status, MemorySegment handle) {
    }

    static OpenResult regOpenKeyExW(Arena arena, long hKey, String subKey, int samDesired) {
        MemorySegment subKeySeg = arena.allocateFrom(subKey, java.nio.charset.StandardCharsets.UTF_16LE);
        MemorySegment outSeg = arena.allocate(ADDRESS);
        try {
            int status = (int) REG_OPEN_KEY_EX_W.invokeExact(
                    MemorySegment.ofAddress(hKey), subKeySeg, 0, samDesired, outSeg);
            return new OpenResult(status, outSeg.get(ADDRESS, 0));
        } catch (Throwable t) {
            throw new RuntimeException("RegOpenKeyExW native call failed", t);
        }
    }

    /**
     * @return the Win32 status code (0 = success, {@link WinConst#ERROR_NO_MORE_ITEMS} when the index is past the last value)
     */
    static int regEnumValueW(Arena arena, MemorySegment hKey, int index,
                             MemorySegment nameBuffer, MemorySegment nameLength,
                             MemorySegment dataBuffer, MemorySegment dataLength) {
        try {
            return (int) REG_ENUM_VALUE_W.invokeExact(
                    hKey, index, nameBuffer, nameLength, MemorySegment.NULL, MemorySegment.NULL, dataBuffer, dataLength);
        } catch (Throwable t) {
            throw new RuntimeException("RegEnumValueW native call failed", t);
        }
    }

    static void regCloseKey(MemorySegment hKey) {
        try {
            int unused = (int) REG_CLOSE_KEY.invokeExact(hKey);
        } catch (Throwable t) {
            throw new RuntimeException("RegCloseKey native call failed", t);
        }
    }
}
