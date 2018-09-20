package net.wbz.selectrix4java.data.recording;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import net.wbz.selectrix4java.bus.BusDataReceiver;
import net.wbz.selectrix4java.data.BusDataChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Player to playback an record to an {@link net.wbz.selectrix4java.bus.BusDataReceiver}.
 *
 * @author Daniel Tuerk
 */
public class BusDataPlayer {

    private static final Logger log = LoggerFactory.getLogger(BusDataPlayer.class);

    /**
     * Receiver for the playback.
     */
    private final BusDataReceiver receiver;

    private final BusDataChannel busDataChannel;

    private final ExecutorService executorService;
    private final List<BusDataPlayerListener> listeners = Lists.newArrayList();
    private final int playbackSpeedMultiplication;
    private transient boolean running = false;

    /**
     * Creating new player to call the given {@link net.wbz.selectrix4java.bus.BusDataReceiver} by playing an record.
     *
     * @param receiver {@link net.wbz.selectrix4java.bus.BusDataReceiver}
     * @param busDataChannel {@link net.wbz.selectrix4java.data.BusDataChannel}
     */
    public BusDataPlayer(BusDataReceiver receiver, BusDataChannel busDataChannel) {
        this(receiver, busDataChannel, 1);
    }

    /**
     * Creating new player to call the given {@link net.wbz.selectrix4java.bus.BusDataReceiver} by playing an record.
     *
     * @param receiver {@link net.wbz.selectrix4java.bus.BusDataReceiver}
     * @param busDataChannel {@link net.wbz.selectrix4java.data.BusDataChannel}
     * @param playbackSpeedMultiplication multiplication of playback speed (1 is normal speed; 2 double speed)
     */
    public BusDataPlayer(BusDataReceiver receiver, BusDataChannel busDataChannel, int playbackSpeedMultiplication) {
        this.receiver = receiver;
        this.busDataChannel = busDataChannel;
        assert playbackSpeedMultiplication >= 0;
        this.playbackSpeedMultiplication = playbackSpeedMultiplication;

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("bus-data-player-%d").build();
        executorService = Executors.newSingleThreadExecutor(namedThreadFactory);
    }

    /**
     * Start to playback the {@link net.wbz.selectrix4java.data.recording.BusDataRecord} from the given record file.
     *
     * @param recordFile {@link java.nio.file.Path} file of the record
     * @throws RecordingException can't playback
     */
    public void start(Path recordFile) throws RecordingException {
        if (Files.exists(recordFile)) {
            try {
                BusDataRecord busDataRecord = new Gson()
                        .fromJson(new String(Files.readAllBytes(recordFile)), BusDataRecord.class);
                start(busDataRecord);
            } catch (IOException e) {
                throw new RecordingException("can't start playback", e);
            }
        } else {
            throw new RecordingException(String.format("record file doesn't exists! (%s)", recordFile.toString()));
        }
    }

    /**
     * Start the player to read the given record and simulate the received bus data.
     *
     * @param record {@link net.wbz.selectrix4java.data.recording.BusDataRecord} to play
     */
    public void start(final BusDataRecord record) {
        if (!record.getEntries().isEmpty()) {
            running = true;
            busDataChannel.pause();
            fireStartEvent();
            executorService.submit(() -> {
                long lastReceivedTime = record.getEntries().get(0).getTimestamp();

                for (BusDataRecordEntry recordEntry : record.getEntries()) {
                    if (!running) {
                        break;
                    }
                    // simulate delay of recorded data by sleeping for the timestamp difference between record
                    long durationInMs = TimeUnit.NANOSECONDS.toMillis(recordEntry.getTimestamp() - lastReceivedTime);
                    durationInMs = durationInMs / playbackSpeedMultiplication;

                    if (durationInMs > 0) {
                        try {
                            Thread.sleep(durationInMs);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    lastReceivedTime = recordEntry.getTimestamp();

                    // handle data
                    receiver.received(recordEntry.getBus(), recordEntry.getData());
                }
                stop();
            });
        } else {
            stop();
            throw new RuntimeException("record to play is empty!");
        }
    }

    /**
     * Stop the player.
     */
    public void stop() {
        if (running) {
            running = false;
            executorService.shutdown();
            fireStopEvent();
            busDataChannel.resume();
        } else {
            log.warn("stop called for non running player");
        }
    }

    private void fireStartEvent() {
        for (final BusDataPlayerListener listener : listeners) {
            new FutureTask<>((Callable<Void>) () -> {
                listener.playbackStarted();
                return null;
            }).run();
        }
    }


    private void fireStopEvent() {
        for (final BusDataPlayerListener listener : listeners) {
            new FutureTask<>((Callable<Void>) () -> {
                listener.playbackStopped();
                return null;
            }).run();
        }
    }

    public void addListener(BusDataPlayerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(BusDataPlayerListener listener) {
        listeners.remove(listener);
    }

}
