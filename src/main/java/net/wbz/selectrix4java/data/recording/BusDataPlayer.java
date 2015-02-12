package net.wbz.selectrix4java.data.recording;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import net.wbz.selectrix4java.bus.BusDataReceiver;
import net.wbz.selectrix4java.data.BusDataChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

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

    private transient boolean running = false;

    private final List<BusDataPlayerListener> listeners = Lists.newArrayList();

    /**
     * Creating new player to call the given {@link net.wbz.selectrix4java.bus.BusDataReceiver} by playing an record.
     *
     * @param receiver {@link net.wbz.selectrix4java.bus.BusDataReceiver}
     */
    public BusDataPlayer(BusDataReceiver receiver, BusDataChannel busDataChannel) {
        this.receiver = receiver;
        this.busDataChannel=busDataChannel;

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("bus-data-player-%d").build();
        executorService = Executors.newSingleThreadExecutor(namedThreadFactory);
    }

    /**
     * Start to playback the {@link net.wbz.selectrix4java.data.recording.BusDataRecord} from the given record file.
     *
     * @param recordFile {@link java.nio.file.Path} file of the record
     */
    public void start(Path recordFile) throws RecordingException {
        if (Files.exists(recordFile)) {
            try {
                BusDataRecord busDataRecord = new Gson().fromJson(new String(Files.readAllBytes(recordFile)), BusDataRecord.class);
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
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    long lastReceivedTime = record.getEntries().get(0).getTimestamp();

                    for (BusDataRecordEntry recordEntry : record.getEntries()) {
                        if (!running) {
                            break;
                        }
                        // simulate delay of recorded data by sleeping for the timestamp difference between record
                        long durationInMs = TimeUnit.NANOSECONDS.toMillis(recordEntry.getTimestamp() - lastReceivedTime);
                        try {
                            Thread.sleep(durationInMs);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        lastReceivedTime = recordEntry.getTimestamp();

                        // handle data
                        receiver.received(recordEntry.getBus(), recordEntry.getData());
                    }
                    stop();
                }
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
            new FutureTask<Void>(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    listener.playbackStarted();
                    return null;
                }
            }).run();
        }
    }


    private void fireStopEvent() {
        for (final BusDataPlayerListener listener : listeners) {
            new FutureTask<>(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    listener.playbackStopped();
                    return null;
                }
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
