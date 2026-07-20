package io.github.danieltuerk.selectrix4java.jna;

public class SerialServiceFactory {

    public static SerialPortService create() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return invoke("io.github.danieltuerk.selectrix4java.jna.win.WindowsSerialService");
        } else {
            return invoke("io.github.danieltuerk.selectrix4java.jna.linux.LinuxSerialService");
        }
    }

    private static SerialPortService invoke(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return (SerialPortService) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
