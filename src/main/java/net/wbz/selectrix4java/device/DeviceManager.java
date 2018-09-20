package net.wbz.selectrix4java.device;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import net.wbz.selectrix4java.device.serial.SerialDevice;
import net.wbz.selectrix4java.device.test.TestDevice;

/**
 * Manager to access the {@link Device} by device id.
 *
 * @author Daniel Tuerk
 */
public class DeviceManager {

    private final Map<String, Device> devices = Maps.newHashMap();

    private final List<DeviceConnectionListener> listeners = Lists.newArrayList();

    public enum DEVICE_TYPE {SERIAL, TEST}

    public Device registerDevice(DEVICE_TYPE type, String deviceId, int baudRate) {
        if (!devices.containsKey(deviceId)) {
            Device device = createDevice(type, deviceId, baudRate);
            for (DeviceConnectionListener listener : listeners) {
                device.addDeviceConnectionListener(listener);
            }
            devices.put(deviceId, device);
        }
        return devices.get(deviceId);
    }

    private Device createDevice(DEVICE_TYPE type, String deviceId, int baudRate) {
        switch (type) {
            case SERIAL:
                return new SerialDevice(deviceId, baudRate);
            case TEST:
                return new TestDevice();
            default:
                throw new RuntimeException("no device found for type " + type.name());
        }
    }

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

    public List<Device> getDevices() {
        return Lists.newArrayList(devices.values());
    }

    public Device getDeviceById(String deviceId) {
        return devices.get(deviceId);
    }

    public List<String> getDeviceIds() {
        return Lists.newArrayList(devices.keySet());
    }

    public Device getConnectedDevice() throws DeviceAccessException {
        for (Device device : devices.values()) {
            if (device.isConnected()) {
                return device;
            }
        }
        throw new DeviceAccessException("no device connected");
    }

    public boolean isConnected() {
        try {
            getConnectedDevice();
            return true;
        } catch (DeviceAccessException e) {
            return false;
        }
    }

    public void removeDevice(Device device) {
        for (Map.Entry<String, Device> entry : devices.entrySet()) {
            if (entry.getValue() == device) {
                devices.remove(entry.getKey());
                return;
            }
        }
        throw new RuntimeException("no device found to delete");
    }


    public void addDeviceConnectionListener(DeviceConnectionListener listener) {
        listeners.add(listener);
        for (Device device : devices.values()) {
            device.addDeviceConnectionListener(listener);
        }
    }

    public void removeDeviceConnectionListener(DeviceConnectionListener listener) {
        listeners.remove(listener);
        for (Device device : devices.values()) {
            device.removeDeviceConnectionListener(listener);
        }
    }

}