package net.wbz.selectrix4java.device;

import net.wbz.selectrix4java.device.Device.SYSTEM_FORMAT;

/**
 * Listener for the system format for a {@link Device}.
 *
 * @author Daniel Tuerk
 */
public interface SystemFormatListener {

    void systemFormatChanged(SYSTEM_FORMAT systemFormat);

}
