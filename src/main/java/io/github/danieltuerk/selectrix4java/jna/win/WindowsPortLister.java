package io.github.danieltuerk.selectrix4java.jna.win;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import io.github.danieltuerk.selectrix4java.jna.SerialPortLister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class WindowsPortLister {
    private static final Logger log = LoggerFactory.getLogger(WindowsPortLister.class);

    public static List<SerialPortLister.PortInfo> list() {

        try {
            var values = Advapi32Util.registryGetValues(
                WinReg.HKEY_LOCAL_MACHINE,
                "HARDWARE\\DEVICEMAP\\SERIALCOMM"
            );

            return values.values().stream()
                .map(Object::toString)
                .map(port -> new SerialPortLister.PortInfo(port, port))
                .toList();

        } catch (Exception e) {
            log.error("can't list ports", e);
            return List.of();
        }
    }
}
