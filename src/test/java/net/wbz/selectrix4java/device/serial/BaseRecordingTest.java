package net.wbz.selectrix4java.device.serial;

import net.wbz.selectrix4java.data.recording.BusDataPlayer;
import net.wbz.selectrix4java.data.recording.BusDataPlayerListener;
import net.wbz.selectrix4java.device.DeviceManager;

import java.nio.file.Paths;

/**
 * @author Daniel Tuerk
 */
public class BaseRecordingTest extends BaseTest {

    private final String recordFilePath;

    private BusDataPlayer busDataPlayer;

    private final boolean[] running = {false};

    private final int playbackSpeed;

    public BaseRecordingTest(String recordFilePath) {
        this(recordFilePath, 1);
    }

    public BaseRecordingTest(String recordFilePath, int playbackSpeed) {
        super(new Connection(DEVICE_ID_TEST, DeviceManager.DEVICE_TYPE.TEST));
        this.recordFilePath = recordFilePath;
        this.playbackSpeed = playbackSpeed;
    }

    @Override
    public void setup() {
        super.setup();

        busDataPlayer = new BusDataPlayer(getDevice().getBusDataDispatcher(), getDevice().getBusDataChannel(), playbackSpeed);


        busDataPlayer.addListener(new BusDataPlayerListener() {
            @Override
            public void playbackStarted() {
                print("playback started - %s", recordFilePath);
                running[0] = true;
            }

            @Override
            public void playbackStopped() {
                print("playback stopped - %s", recordFilePath);
                running[0] = false;
            }
        });
    }

    public void startPlayback() throws Exception {
        busDataPlayer.start(Paths.get(ClassLoader.getSystemResource(recordFilePath).toURI()));
    }

    public void waitToFinishRecord() throws InterruptedException {
        while (running[0]) {
            Thread.sleep(100L);
        }
    }

    @Override
    public void tearDown() {
        super.tearDown();
    }
}
