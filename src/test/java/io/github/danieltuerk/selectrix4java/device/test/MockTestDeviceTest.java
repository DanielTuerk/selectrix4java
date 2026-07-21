package io.github.danieltuerk.selectrix4java.device.test;

import io.github.danieltuerk.selectrix4java.bus.consumption.AllBusDataConsumer;
import io.github.danieltuerk.selectrix4java.device.Device;
import io.github.danieltuerk.selectrix4java.device.DeviceAccessException;
import io.github.danieltuerk.selectrix4java.device.DeviceConnectionListener;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Tuerk
 */
public class MockTestDeviceTest {
    private static final Logger log = LoggerFactory.getLogger(MockTestDeviceTest.class);
    boolean callbackReceived = false;

    @Test
    public void test() throws DeviceAccessException, InterruptedException {
        final byte valueUnderTest = (byte) 50;
        final byte addressUnderTest = (byte) 10;
        final int busUnderTest = 1;
        Device device = new TestDevice("test");
        device.getBusDataDispatcher().registerConsumer(new AllBusDataConsumer() {
            @Override
            public void valueChanged(int bus, int address, int oldValue, int newValue) {
                System.out.printf("%d: %d=%d%n", bus, address, newValue);
                if (bus == busUnderTest && address == addressUnderTest && newValue == valueUnderTest && oldValue == 0) {
                    callbackReceived = true;
                }
            }
        });
        device.addDeviceConnectionListener(new DeviceConnectionListener() {
            @Override
            public void connected(Device device) {
                try {
                    device.getBusAddress(busUnderTest, addressUnderTest).sendData(valueUnderTest);
                } catch (DeviceAccessException e) {
                    log.error("can't get bus address", e);
                    assert false;
                }

            }

            @Override
            public void disconnected(Device device) {

            }
        });

        device.connect();

        Thread.sleep(1000L);

        device.disconnect();

        Assert.assertTrue(callbackReceived);
    }
}
