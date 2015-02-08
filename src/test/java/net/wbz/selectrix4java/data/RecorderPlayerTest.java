package net.wbz.selectrix4java.data;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import net.wbz.selectrix4java.bus.TestDataSet;
import net.wbz.selectrix4java.bus.consumption.BusAddressDataConsumer;
import net.wbz.selectrix4java.data.recording.IsRecordable;
import net.wbz.selectrix4java.data.recording.RecordingException;
import net.wbz.selectrix4java.device.DeviceAccessException;
import net.wbz.selectrix4java.device.DeviceManager;
import net.wbz.selectrix4java.device.serial.BaseTest;
import net.wbz.selectrix4java.device.serial.Connection;
import net.wbz.selectrix4java.device.test.BusDataPlayer;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Test to record the bus of the device and playback the record as device data.
 *
 * @author Daniel Tuerk
 */
public class RecorderPlayerTest extends BaseTest {

    private final static Path RECORD_PATH_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "selectrix-test");
    private Path recordDestination;

    /**
     * Use the connection of the {@link net.wbz.selectrix4java.device.test.TestDevice}.
     */
    public RecorderPlayerTest() {
        super(new Connection(DEVICE_ID_TEST, DeviceManager.DEVICE_TYPE.TEST));
    }

    @Test
    public void testRecording() throws DeviceAccessException, InterruptedException, RecordingException {

        List<TestDataSet> testDataSets = Lists.newArrayList(new TestDataSet(0, 2, 2), new TestDataSet(1, 2, 4), new TestDataSet(1, 4, 8));


        IsRecordable recordableDevice = (IsRecordable) getDevice();
        recordableDevice.startRecording(RECORD_PATH_DIR);


        for (TestDataSet testDataSet : testDataSets) {
            print("send -> bus %d address %d: %d", testDataSet.getSendBus(), testDataSet.getSendAddress(), testDataSet.getSendValue());
            getDevice().getBusAddress(testDataSet.getSendBus(), (byte) testDataSet.getSendAddress()).sendData((byte) testDataSet.getSendValue());
            Thread.sleep(200L);
        }

        recordDestination = recordableDevice.stopRecording();
        Assert.assertNotNull(recordDestination);


        BusDataPlayer busDataPlayer = new BusDataPlayer(getDevice().getBusDataDispatcher());

        for (final TestDataSet testDataSet : testDataSets) {
            getDevice().getBusDataDispatcher().registerConsumer(new BusAddressDataConsumer(testDataSet.getSendBus(), testDataSet.getSendAddress()) {
                @Override
                public void valueChanged(int oldValue, int newValue) {
                    printData(oldValue, newValue, getBus(), getAddress());
                    testDataSet.setResult(getBus(), getAddress(), newValue);
                }
            });
        }

        busDataPlayer.start(recordDestination);

        Thread.sleep(2000L);

        busDataPlayer.stop();

        for (TestDataSet testDataSet : testDataSets) {
            assertEventReceived(testDataSet, 1);
        }

    }


    @Override
    public void tearDown() {
        super.tearDown();

        // delete temp record file
        try {
            Files.delete(recordDestination);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
