package net.wbz.selectrix4java.device;

/**
 * Listener for the rail voltage state for a {@link Device}.
 *
 * @author Daniel Tuerk
 */
public interface RailVoltageListener {

    /**
     * The new rail voltage state.
     *
     * @param isOn {@code true} if on otherwise off
     */
    void changed(boolean isOn);
}
