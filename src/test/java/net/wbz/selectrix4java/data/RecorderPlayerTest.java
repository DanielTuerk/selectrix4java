package net.wbz.selectrix4java.data;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import net.wbz.selectrix4java.bus.TestDataSet;
import net.wbz.selectrix4java.bus.consumption.BusAddressDataConsumer;
import net.wbz.selectrix4java.data.recording.BusDataPlayer;
import net.wbz.selectrix4java.data.recording.BusDataPlayerListener;
import net.wbz.selectrix4java.data.recording.IsRecordable;
import net.wbz.selectrix4java.data.recording.RecordingException;
import net.wbz.selectrix4java.device.DeviceAccessException;
import net.wbz.selectrix4java.device.serial.BaseTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test to record the bus of the device and playback the record as device data.
 *
 * @author Daniel Tuerk
 */
public class RecorderPlayerTest extends BaseTest {

    private final static Path RECORD_PATH_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "selectrix-test");
    private static final long PLAYER_TIMEOUT = 10000L;
    private Path recordDestination;

    @Test
    public void test1Recording() throws DeviceAccessException, InterruptedException, RecordingException {
        List<TestDataSet> testDataSets = Lists
                .newArrayList(new TestDataSet(0, 3, 2), new TestDataSet(1, 2, 4), new TestDataSet(1, 4, 8));
        IsRecordable recordableDevice = (IsRecordable) getDevice();
        recordableDevice.startRecording(RECORD_PATH_DIR);

        System.out.println("start sending test data");
        for (TestDataSet testDataSet : testDataSets) {
            print("send -> bus %d address %d: %d", testDataSet.getSendBus(), testDataSet.getSendAddress(),
                    testDataSet.getSendValue());
            getDevice().getBusAddress(testDataSet.getSendBus(), (byte) testDataSet.getSendAddress())
                    .sendData((byte) testDataSet.getSendValue());
            Thread.sleep(200L);
        }
        System.out.println("finished sending test data");

        recordDestination = recordableDevice.stopRecording();
        Assert.assertNotNull(recordDestination);

        // reconnect device to get an empty bus
        super.tearDown();
        super.setup();

        // init and start player
        BusDataPlayer busDataPlayer = new BusDataPlayer(getDevice().getBusDataDispatcher(),
                getDevice().getBusDataChannel());
        final boolean[] running = {false};
        for (final TestDataSet testDataSet : testDataSets) {
            getDevice().getBusDataDispatcher().registerConsumer(
                    new BusAddressDataConsumer(testDataSet.getSendBus(), testDataSet.getSendAddress()) {
                        @Override
                        public void valueChanged(int oldValue, int newValue) {
                            printData(running[0] ? "player" : "init", oldValue, newValue, getBus(), getAddress());
                            if (running[0]) {
                                testDataSet.setResult(getBus(), getAddress(), newValue);
                            }
                        }
                    });
        }
        busDataPlayer.addListener(new BusDataPlayerListener() {
            @Override
            public void playbackStarted() {
                System.out.println("playbackStarted");
                running[0] = true;
            }

            @Override
            public void playbackStopped() {
                System.out.println("playbackStopped");
                running[0] = false;
            }
        });
        busDataPlayer.start(recordDestination);

        // wait for player
        long timeout = System.currentTimeMillis() + PLAYER_TIMEOUT;
        while (System.currentTimeMillis() < timeout && running[0]) {
            Thread.sleep(100L);
        }

        // assert test data
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
