package net.wbz.selectrix4java.device.serial;

import net.wbz.selectrix4java.data.recording.BusDataPlayer;
import net.wbz.selectrix4java.data.recording.BusDataPlayerListener;
import net.wbz.selectrix4java.device.DeviceManager;

import java.nio.file.Paths;

/**
 * @author Daniel Tuerk
 */
public class BaseRecordingTest extends BaseTest {

    protected static final int DEFAULT_PLAYBACK_SPEED=10;
    protected static final int NORMAL_PLAYBACK_SPEED=1;

    private final String recordFilePath;

    private BusDataPlayer busDataPlayer;

    private final boolean[] running = {false};

    private final int playbackSpeed;

    public BaseRecordingTest(String recordFilePath) {
        this(recordFilePath, NORMAL_PLAYBACK_SPEED);
    }

    public BaseRecordingTest(String recordFilePath, int playbackSpeed) {
        this.recordFilePath = recordFilePath;
        this.playbackSpeed = playbackSpeed;
    }

    @Override
    public void setup() throws InterruptedException {
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
