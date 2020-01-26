package net.wbz.selectrix4java.device.serial;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.wbz.selectrix4java.device.Device;
import net.wbz.selectrix4java.device.DeviceAccessException;
import net.wbz.selectrix4java.device.DeviceConnectionListener;
import net.wbz.selectrix4java.device.DeviceManager;

/**
 * @author Daniel Tuerk
 */
public class Connection {

    public final static String DEVICE_ID_TEST = "test";
    private static final int TIMEOUT = 5;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final Device device;
    private volatile boolean connectedCallbackResult;

    public static Connection createTestDeviceConnection() {
        return new Connection(DEVICE_ID_TEST, DeviceManager.DEVICE_TYPE.TEST);
    }

    public Connection(String deviceId, DeviceManager.DEVICE_TYPE deviceType) {
        device = new DeviceManager().createDevice(deviceType, deviceId, SerialDevice.DEFAULT_BAUD_RATE_FCC);
        device.addDeviceConnectionListener(new DeviceConnectionListener() {
            @Override
            public void connected(Device device) {
                connectedCallbackResult = true;
                System.out.println("device connected");
            }

            @Override
            public void disconnected(Device device) {
                connectedCallbackResult = false;
                System.out.println("device disconnected");
            }
        });
    }

    public boolean connect() {
        try {
            return executorService.submit(() -> {
                try {
                    device.connect();
                } catch (DeviceAccessException e) {
                    e.printStackTrace();
                    return false;
                }
                while (!connectedCallbackResult) {
                    Thread.sleep(200L);
                }
                return true;
            }).get(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean disconnect() {
        if (device != null && device.isConnected()) {
            try {
                return executorService.submit(() -> {
                    try {
                        device.disconnect();
                    } catch (DeviceAccessException e) {
                        e.printStackTrace();
                        return false;
                    }
                    while (connectedCallbackResult) {
                        Thread.sleep(200L);
                    }
                    return true;
                }).get(TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
            return false;
        }
        return false;
    }

    public Device getDevice() {
        return device;
    }
}
