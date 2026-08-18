package io.github.danieltuerk.selectrix4java.device;

import io.github.danieltuerk.selectrix4java.device.serial.SerialDevice;
import io.github.danieltuerk.selectrix4java.device.test.TestDevice;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manager to access the {@link Device} by device id.
 *
 * @author Daniel Tuerk
 */
public class DeviceManager {

    private final Map<String, Device> devices = new HashMap<>();

    private final List<DeviceConnectionListener> listeners = new ArrayList<>();

    /**
     * Create manager without registered devices.
     */
    public DeviceManager() {
    }

    /**
     * Supported device types.
     */
    public enum DEVICE_TYPE {
        /**
         * Serial device (COM/USB).
         */
        SERIAL,
        /**
         * In-memory test device.
         */
        TEST
    }

    /**
     * Register the given device and forward already registered connection listeners to it.
     *
     * @param device device to register
     */
    public void registerDevice(Device device) {
        if (!devices.containsKey(device.getDeviceId())) {
            for (DeviceConnectionListener listener : listeners) {
                device.addDeviceConnectionListener(listener);
            }
            devices.put(device.getDeviceId(), device);
        }
    }

    /**
     * Create a new device of the given type.
     *
     * @param type type of the device to create
     * @param deviceId OS device id
     * @param baudRate baud rate of the device
     * @return created device
     */
    public Device createDevice(DEVICE_TYPE type, String deviceId, int baudRate) {
        return switch (type) {
            case SERIAL -> new SerialDevice(deviceId, baudRate);
            case TEST -> new TestDevice(deviceId);
        };
    }

    /**
     * Id of the given, registered device.
     *
     * @param device registered device
     * @return device id
     */
    public String getDeviceId(Device device) {
        if (devices.containsValue(device)) {
            for (Map.Entry<String, Device> entry : devices.entrySet()) {
                if (entry.getValue() == device) {
                    return entry.getKey();
                }
            }
        }
        throw new RuntimeException("no key for value");
    }

    /**
     * All registered devices.
     *
     * @return devices
     */
    public List<Device> getDevices() {
        return new ArrayList<>(devices.values());
    }

    /**
     * Registered device for the given id.
     *
     * @param deviceId id of the device
     * @return device, if registered
     */
    public Optional<Device> getDeviceById(String deviceId) {
        return Optional.ofNullable(devices.get(deviceId));
    }

    /**
     * Ids of all registered devices.
     *
     * @return device ids
     */
    public List<String> getDeviceIds() {
        return new ArrayList<>(devices.keySet());
    }

    /**
     * First registered device with an active connection.
     *
     * @return connected device, if any
     */
    public Optional<Device> getConnectedDevice() {
        for (Device device : devices.values()) {
            if (device.isConnected()) {
                return Optional.of(device);
            }
        }
        return Optional.empty();
    }

    /**
     * Check whether at least one registered device is connected.
     *
     * @return {@code true} if a device is connected
     */
    public boolean isConnected() {
        return getConnectedDevice().isPresent();
    }

    /**
     * Remove the given, already registered device.
     *
     * @param device device to remove
     */
    public void removeDevice(Device device) {
        for (Map.Entry<String, Device> entry : devices.entrySet()) {
            if (entry.getValue() == device) {
                devices.remove(entry.getKey());
                return;
            }
        }
        throw new RuntimeException("no device found to delete");
    }

    /**
     * Register the given listener to all managed devices.
     *
     * @param listener listener to add
     */
    public void addDeviceConnectionListener(DeviceConnectionListener listener) {
        listeners.add(listener);
        devices.values()
            .stream()
            // sort by disconnected devices first
            .sorted(Comparator.comparing(Device::isConnected))
            // add listeners and receive the initial connection state
            .forEach(device -> device.addDeviceConnectionListener(listener));
    }

    /**
     * Remove the given, already registered listener from all managed devices.
     *
     * @param listener listener to remove
     */
    public void removeDeviceConnectionListener(DeviceConnectionListener listener) {
        listeners.remove(listener);
        devices.values().forEach(device -> device.removeDeviceConnectionListener(listener));
    }

}