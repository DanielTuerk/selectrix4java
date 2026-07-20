package io.github.danieltuerk.selectrix4java.jna;

import java.util.List;

public class SerialPortLister {

    public record PortInfo(String path, String name) {
    }

    public static List<PortInfo> list() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return invoke("io.github.danieltuerk.selectrix4java.jna.win.WindowsPortLister");
        } else {
            return invoke("io.github.danieltuerk.selectrix4java.jna.linux.LinuxPortLister");
        }
    }

    @SuppressWarnings("unchecked")
    private static List<PortInfo> invoke(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return (List<PortInfo>) clazz.getMethod("list").invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}