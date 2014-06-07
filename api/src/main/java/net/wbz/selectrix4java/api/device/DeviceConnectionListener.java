package net.wbz.selectrix4java.api.device;

/**
 * Listener for the state of the connection for the {@link net.wbz.selectrix4java.api.device.Device}s.
 *
 * @author daniel.tuerk@w-b-z.com
 */
public interface DeviceConnectionListener {

    /**
     * The {@link net.wbz.selectrix4java.api.device.Device} is connected.
     *
     * @param device {@link net.wbz.selectrix4java.api.device.Device}
     */
    public void connected(Device device);

    /**
     * The {@link net.wbz.selectrix4java.api.device.Device} is disconnected.
     *
     * @param device {@link net.wbz.selectrix4java.api.device.Device}
     */
    public void disconnected(Device device);
}
