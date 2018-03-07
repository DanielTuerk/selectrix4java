package net.wbz.selectrix4java.device;

/**
 * Listener for the state of the connection for the {@link net.wbz.selectrix4java.device.Device}s.
 *
 * @author daniel.tuerk@w-b-z.com
 */
public interface DeviceConnectionListener {

    /**
     * The {@link net.wbz.selectrix4java.device.Device} is connected.
     *
     * @param device {@link net.wbz.selectrix4java.device.Device}
     */
    void connected(Device device);

    /**
     * The {@link net.wbz.selectrix4java.device.Device} is disconnected.
     *
     * @param device {@link net.wbz.selectrix4java.device.Device}
     */
    void disconnected(Device device);
}
