package io.github.danieltuerk.selectrix4java.device.serial;

import io.github.danieltuerk.selectrix4java.device.serial.ffm.linux.LinuxPortLister;
import io.github.danieltuerk.selectrix4java.device.serial.ffm.win.WindowsPortLister;

import java.util.List;

/**
 * Lists the serial ports available on the current OS.
 *
 * @author Daniel Tuerk
 */
public class SerialPortLister {

    private SerialPortLister() {
    }

    /**
     * Information about an available serial port.
     *
     * @param path OS path or identifier of the port
     * @param name display name of the port
     */
    public record PortInfo(String path, String name) {
    }

    /**
     * List the serial ports available on the current OS.
     *
     * @return available ports
     */
    public static List<PortInfo> list() {
        var infos = OperatingSystem.isWindows() ? WindowsPortLister.list() : LinuxPortLister.list();
        return infos.stream().map(info -> new PortInfo(info.path(), info.name())).toList();
    }
}