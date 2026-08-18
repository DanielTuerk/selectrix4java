package io.github.danieltuerk.selectrix4java.device.serial;

import io.github.danieltuerk.selectrix4java.device.serial.ffm.SerialPortService;
import io.github.danieltuerk.selectrix4java.device.serial.ffm.linux.LinuxSerialService;
import io.github.danieltuerk.selectrix4java.device.serial.ffm.win.WindowsSerialService;

/**
 * Creates the {@link SerialPortService} implementation for the current OS.
 *
 * @author Daniel Tuerk
 */
public class SerialServiceFactory {

    private SerialServiceFactory() {
    }

    /**
     * Create the {@link SerialPortService} implementation for the current OS.
     *
     * @return service implementation
     */
    public static SerialPortService create() {
        return OperatingSystem.isWindows() ? new WindowsSerialService() : new LinuxSerialService();
    }

}
