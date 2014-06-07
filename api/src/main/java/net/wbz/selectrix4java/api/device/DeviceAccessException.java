package net.wbz.selectrix4java.api.device;

/**
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class DeviceAccessException extends Exception {
    public DeviceAccessException(String s) {
        super(s);
    }

    public DeviceAccessException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
