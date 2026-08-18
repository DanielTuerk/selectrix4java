package io.github.danieltuerk.selectrix4java.device.serial.ffm.win;

import io.github.danieltuerk.selectrix4java.device.serial.ffm.SerialPortInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class WindowsPortLister {

    private static final Logger log = LoggerFactory.getLogger(WindowsPortLister.class);

    private static final String SERIALCOMM_KEY = "HARDWARE\\DEVICEMAP\\SERIALCOMM";
    private static final int NAME_BUFFER_CHARS = 256;
    private static final int DATA_BUFFER_BYTES = 512;

    public static List<SerialPortInfo> list() {

        try (Arena arena = Arena.ofConfined()) {

            Advapi32.OpenResult open = Advapi32.regOpenKeyExW(
                    arena, WinConst.HKEY_LOCAL_MACHINE, SERIALCOMM_KEY, WinConst.KEY_READ);

            if (open.status() != 0) {
                return List.of();
            }

            MemorySegment hKey = open.handle();
            try {
                return enumerateValues(arena, hKey);
            } finally {
                Advapi32.regCloseKey(hKey);
            }

        } catch (Exception e) {
            log.error("can't list ports", e);
            return List.of();
        }
    }

    private static List<SerialPortInfo> enumerateValues(Arena arena, MemorySegment hKey) {

        List<SerialPortInfo> result = new ArrayList<>();

        for (int index = 0; ; index++) {

            MemorySegment nameBuffer = arena.allocate((long) NAME_BUFFER_CHARS * 2);
            MemorySegment nameLength = arena.allocate(JAVA_INT);
            nameLength.set(JAVA_INT, 0, NAME_BUFFER_CHARS);

            MemorySegment dataBuffer = arena.allocate(DATA_BUFFER_BYTES);
            MemorySegment dataLength = arena.allocate(JAVA_INT);
            dataLength.set(JAVA_INT, 0, DATA_BUFFER_BYTES);

            int status = Advapi32.regEnumValueW(arena, hKey, index, nameBuffer, nameLength, dataBuffer, dataLength);

            if (status == WinConst.ERROR_NO_MORE_ITEMS) {
                break;
            }
            if (status != 0) {
                log.warn("RegEnumValueW failed at index {} with status {}", index, status);
                break;
            }

            String value = decodeUtf16(dataBuffer, dataLength.get(JAVA_INT, 0));
            result.add(new SerialPortInfo(value, value));
        }

        return result;
    }

    private static String decodeUtf16(MemorySegment segment, int byteLength) {
        byte[] bytes = new byte[byteLength];
        MemorySegment.copy(segment, JAVA_BYTE, 0, bytes, 0, byteLength);
        String value = new String(bytes, StandardCharsets.UTF_16LE);
        int nullIndex = value.indexOf('\0');
        return nullIndex >= 0 ? value.substring(0, nullIndex) : value;
    }
}
