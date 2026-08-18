package io.github.danieltuerk.selectrix4java.device.serial.ffm.linux;

import io.github.danieltuerk.selectrix4java.device.serial.ffm.SerialPortInfo;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class LinuxPortLister {

    public static List<SerialPortInfo> list() {
        File dir = new File("/dev/serial/by-id");
        if (dir.exists()) {
            File[] files = dir.listFiles();

            if (files != null) {
                return Arrays.stream(files)
                    .map(f -> new SerialPortInfo(
                        resolvePath(f),
                        f.getName()
                    ))
                    .toList();
            }
        }
        return List.of();
    }

    private static String resolvePath(File f) {
        try {
            return f.getCanonicalPath();
        } catch (IOException e) {
            return f.getAbsolutePath();
        }
    }
}
