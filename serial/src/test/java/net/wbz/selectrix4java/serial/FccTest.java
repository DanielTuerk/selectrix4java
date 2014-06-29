package net.wbz.selectrix4java.serial;

import net.wbz.selectrix4java.api.device.Device;
import net.wbz.selectrix4java.SerialDevice;
import net.wbz.selectrix4java.api.device.DeviceAccessException;
import net.wbz.selectrix4java.api.device.DeviceConnectionListener;
import net.wbz.selectrix4java.api.train.TrainModule;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

/**
 * Test for an connected FCC.
 *
 * FIXME: Useless to execute in maven build
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class FccTest {

    private final static String DEVICE_ID = "/dev/tty.usbserial-145";

    private Device device;

    public void setup() throws IOException {

//        Assert.assertTrue(device.getRailVoltage());
    }
    @Ignore
    public void testRailVoltage() throws IOException {
//        device.setRailVoltage(true);
//        Assert.assertTrue(device.getRailVoltage());
//        device.setRailVoltage(false);
//        Assert.assertFalse(device.getRailVoltage());
    }

    @Ignore
//    @Test
    public void testTrain() throws DeviceAccessException, InterruptedException, IOException {
        device = new SerialDevice(DEVICE_ID, SerialDevice.DEFAULT_BAUD_RATE_FCC);
        device.addDeviceConnectionListener(new DeviceConnectionListener() {
            @Override
            public void connected(Device device) {
                System.out.println("device callback connected");
            }

            @Override
            public void disconnected(Device device) {

            }
        });

        device.connect();
        Assert.assertTrue(device.isConnected());

        Thread.sleep(1000L);

        device.setRailVoltage(true);

        Thread.sleep(1000L);

        TrainModule trainModule = device.getTrainModule((byte) 3);
        trainModule.setLight(true);
        trainModule.setDirection(TrainModule.DRIVING_DIRECTION.FORWARD);
        Thread.sleep(1000L);

        trainModule.setDrivingLevel(4);
        Thread.sleep(2000L);

        trainModule.setDrivingLevel(0);
        trainModule.setHorn(true);
        Thread.sleep(1000L);
        trainModule.setHorn(false);

        trainModule.setDirection(TrainModule.DRIVING_DIRECTION.BACKWARD);

        trainModule.setDrivingLevel(4);
        Thread.sleep(2000L);

        trainModule.setDrivingLevel(0);
        Thread.sleep(1000L);

        trainModule.setLight(false);

        Thread.sleep(1000L);

        device.setRailVoltage(false);
//        Assert.assertFalse(device.getRailVoltage());
        device.disconnect();
        Assert.assertFalse(device.isConnected());
    }

    public void tearDown() throws IOException {

    }

}
