package net.wbz.selectrix4java.jna.linux;

import net.wbz.selectrix4java.jna.SerialPortLister;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class LinuxPortLister {

    public static List<SerialPortLister.PortInfo> list() {
        File dir = new File("/dev/serial/by-id");
        if (dir.exists()) {
            File[] files = dir.listFiles();

            if (files != null) {
                return Arrays.stream(files)
                    .map(f -> new SerialPortLister.PortInfo(
                        f.getAbsolutePath(),
                        f.getName()
                    ))
                    .toList();
            }
        }
        return List.of();
    }
}
