package net.wbz.selectrix4java.device;

/**
 * @author Daniel Tuerk
 */
public class DeviceAccessException extends Exception {

    public DeviceAccessException(String s) {
        super(s);
    }

    public DeviceAccessException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
