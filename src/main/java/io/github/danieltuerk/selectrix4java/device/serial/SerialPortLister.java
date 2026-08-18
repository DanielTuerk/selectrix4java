package io.github.danieltuerk.selectrix4java.device.serial;

import io.github.danieltuerk.selectrix4java.device.serial.ffm.linux.LinuxPortLister;
import io.github.danieltuerk.selectrix4java.device.serial.ffm.win.WindowsPortLister;

import java.util.List;

public class SerialPortLister {

    public record PortInfo(String path, String name) {
    }

    public static List<PortInfo> list() {
        var infos = OperatingSystem.isWindows() ? WindowsPortLister.list() : LinuxPortLister.list();
        return infos.stream().map(info -> new PortInfo(info.path(), info.name())).toList();
    }
}