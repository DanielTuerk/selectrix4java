package net.wbz.selectrix4java.device.serial;

import net.wbz.selectrix4java.device.Device;
import net.wbz.selectrix4java.device.DeviceAccessException;
import net.wbz.selectrix4java.device.DeviceListener;
import net.wbz.selectrix4java.device.DeviceManager;
import net.wbz.selectrix4java.train.TrainDataListener;
import net.wbz.selectrix4java.train.TrainModule;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

/**
 * Test for an connected FCC.
 * <p/>
 * FIXME: Useless to execute in maven build
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class FccTest extends BaseTest {


    public FccTest() {
        super(new Connection(DEVICE_ID, DeviceManager.DEVICE_TYPE.SERIAL));
    }


    @Test
    public void testSystemFormat() throws InterruptedException, DeviceAccessException {
        final boolean[] roundTrip = {false};
        final Device.SYSTEM_FORMAT initial = getDevice().getActualSystemFormat();
        getDevice().addDeviceConnectionListener(new DeviceListener() {
            @Override
            public void systemFormatChanged(Device.SYSTEM_FORMAT actualSystemFormat) {
                print("system format = %s", actualSystemFormat.name());
                if (initial == actualSystemFormat) {
                    roundTrip[0] = true;
                }
            }

            @Override
            public void connected(Device device) {

            }

            @Override
            public void disconnected(Device device) {

            }
        });
        while (!roundTrip[0]){
            getDevice().switchDeviceSystemFormat();
            Thread.sleep(1000L);
            print("get actual: %s",getDevice().getActualSystemFormat().name());
        }
    }

    @Override
    public void tearDown() {
        //TODO
    }

    @Ignore
    public void testRailVoltage()
            throws IOException {
//        device.setRailVoltage(true);
//        Assert.assertTrue(device.getRailVoltage());
//        device.setRailVoltage(false);
//        Assert.assertFalse(device.getRailVoltage());
    }

    @Ignore
    @Test
    public void testTrain()
            throws DeviceAccessException, InterruptedException, IOException {
        Device device = getDevice();

        device.setRailVoltage(true);

        Thread.sleep(1000L);

        TrainModule trainModule = device.getTrainModule((byte) 7);

        trainModule.setHorn(false);
        trainModule.setDrivingLevel(0);
        trainModule.setDirection(TrainModule.DRIVING_DIRECTION.FORWARD);
        trainModule.setLight(false);


        trainModule.addTrainDataListener(new TrainDataListener() {
            @Override
            public void drivingLevelChanged(int level) {
                System.out.println("drivingLevelChanged " + level);
            }

            @Override
            public void drivingDirectionChanged(TrainModule.DRIVING_DIRECTION direction) {
                System.out.println("drivingDirectionChanged " + direction.name());
            }

            @Override
            public void functionStateChanged(byte address, int functionBit, boolean state) {
                System.out.println(String.format("functionStateChanged a: %d f: %d s:%b", address, functionBit, state));
            }

            @Override
            public void lightStateChanged(boolean on) {
                System.out.println("lightStateChanged " + on);

            }

            @Override
            public void hornStateChanged(boolean on) {
                System.out.println("hornStateChanged " + on);
            }
        });
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

        trainModule.setDrivingLevel(12);
        Thread.sleep(2000L);

        trainModule.setDrivingLevel(8);
        Thread.sleep(1000L);

        trainModule.setLight(false);

        Thread.sleep(1000L);

        device.setRailVoltage(false);
//        Assert.assertFalse(device.getRailVoltage());
//    device.disconnect();
//    Assert.assertFalse(device.isConnected());
    }


}
