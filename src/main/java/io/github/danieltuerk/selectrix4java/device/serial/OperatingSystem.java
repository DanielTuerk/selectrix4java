package io.github.danieltuerk.selectrix4java.device.serial;

final class OperatingSystem {

    private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    private OperatingSystem() {
    }

    static boolean isWindows() {
        return WINDOWS;
    }
}
