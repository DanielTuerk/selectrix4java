package net.wbz.selectrix4java.device;

/**
 * Listener for the state of the connection for the {@link Device}s.
 *
 * @author daniel.tuerk@w-b-z.com
 */
public interface DeviceListener extends DeviceConnectionListener {

    /**
     * The {@link Device} changed the system format.
     *
     * @param actualSystemFormat actual system format of the {@link Device}
     */
    void systemFormatChanged(Device.SYSTEM_FORMAT actualSystemFormat);

}
