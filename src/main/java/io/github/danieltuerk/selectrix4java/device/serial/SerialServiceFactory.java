package io.github.danieltuerk.selectrix4java.device.serial;

import io.github.danieltuerk.selectrix4java.device.serial.ffm.SerialPortService;
import io.github.danieltuerk.selectrix4java.device.serial.ffm.linux.LinuxSerialService;
import io.github.danieltuerk.selectrix4java.device.serial.ffm.win.WindowsSerialService;

public class SerialServiceFactory {

    public static SerialPortService create() {
        return OperatingSystem.isWindows() ? new WindowsSerialService() : new LinuxSerialService();
    }

}
