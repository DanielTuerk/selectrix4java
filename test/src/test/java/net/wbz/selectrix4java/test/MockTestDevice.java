package net.wbz.selectrix4java.test;

import net.wbz.selectrix4java.api.bus.AllBusDataConsumer;
import net.wbz.selectrix4java.api.device.Device;
import net.wbz.selectrix4java.api.device.DeviceAccessException;
import net.wbz.selectrix4java.api.device.DeviceConnectionListener;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class MockTestDevice {
     boolean callbackReceived=false;
    @Test
    public void test() throws DeviceAccessException, InterruptedException {

        final byte valueUnderTest = (byte) 11;
        final byte addressUnderTest = (byte) 10;
        final int busUnderTest = 1;
        Device device = new TestDevice();
        device.getBusDataDispatcher().registerConsumer(new AllBusDataConsumer() {
            @Override
            public void valueChanged(int bus, int address, int oldValue, int newValue) {
                System.out.printf("%d: %d=%d%n", bus, address, newValue);
                Assert.assertEquals(busUnderTest, bus);
                Assert.assertEquals(addressUnderTest, address);
                Assert.assertEquals(valueUnderTest, newValue);
                callbackReceived=true;
            }
        });
        device.addDeviceConnectionListener(new DeviceConnectionListener() {
            @Override
            public void connected(Device device) {
                try {
                    device.getBusAddress(busUnderTest, addressUnderTest).sendData(valueUnderTest);
                } catch (DeviceAccessException e) {
                    e.printStackTrace();
                    assert false;
                }

            }

            @Override
            public void disconnected(Device device) {

            }
        });

        device.connect();

        Thread.sleep(2000L);

        device.disconnect();

        Assert.assertTrue(callbackReceived);
    }
}
