package net.wbz.selectrix4java.data;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import net.wbz.selectrix4java.bus.BusDataReceiver;
import net.wbz.selectrix4java.data.recording.BusDataRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The channel communicate with the device to execute read and write operations. Each operation is an {@link
 * net.wbz.selectrix4java.data.AbstractSerialAccessTask}. The tasks can be put into the queue to execute by calling
 * {@link net.wbz.selectrix4java.data.BusDataChannel#send(BusData)}. The queue is polled after a delay to execute an
 * task. If no task is present in the queue, the channel send the {@link ReadBlockTask} to read the actual values from
 * the SX bus. State changes of the values are published to the given {@link net.wbz.selectrix4java.bus.BusDataReceiver}.
 *
 * @author Daniel Tuerk
 */
public class BusDataChannel {

    /**
     * Delay between each task.
     */
    public static final long DELAY_IN_MS = 77L;
    //public static final long DELAY_IN_MS = 55L;

    private static final Logger log = LoggerFactory.getLogger(BusDataChannel.class);
    /**
     * Max errors to receive until shutdown channel.
     */
    private static final int MAX_ERROR_COUNT = 2;
    /**
     * Queue to execute the tasks as FIFO.
     */
    private final Deque<AbstractSerialAccessTask> queue = new LinkedBlockingDeque<>();
    private final ScheduledExecutorService scheduledExecutorService;
    private final ExecutorService serialTaskExecutor;

    /**
     * Output stream of the connected device to write data.
     */
    private final OutputStream outputStream;

    /**
     * Input stream of the connected device to read the data.
     */
    private final InputStream inputStream;
    /**
     * Receivers which are called by reading the input stream of the device by the {@link
     * net.wbz.selectrix4java.data.ReadBlockTask}.
     */
    private final List<BusDataReceiver> receivers = Collections.synchronizedList(new LinkedList<>());
    /**
     * Callback for the state of the channel to open and close the channel.
     */
    private ChannelStateCallback callback;
    /**
     * Flag to pause the channel.
     */
    private transient boolean paused = false;

    /**
     * Count of bus communication errors in a row.
     */
    private int errorCount = 0;

    /**
     * Create an new channel for the given IO streams of the connected device. Default {@link
     * net.wbz.selectrix4java.bus.BusDataReceiver} must be set. Additional receivers can be added at runtime. {@link
     * #addBusDataReceiver}
     *
     * @param inputStream opened {@link java.io.InputStream}
     * @param outputStream opened {@link java.io.OutputStream}
     * @param receiver {@link net.wbz.selectrix4java.bus.BusDataReceiver} to receive the values of the read operations
     */
    public BusDataChannel(InputStream inputStream, OutputStream outputStream, BusDataReceiver receiver) {
        this.outputStream = outputStream;
        this.inputStream = inputStream;
        this.receivers.add(receiver);

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("serial-io-executor-%d").build();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);
        serialTaskExecutor = Executors.newSingleThreadExecutor(namedThreadFactory);
    }

    /**
     * Start the channel to schedule to read the stream with the {@link #DELAY_IN_MS}. Or execute an queued send
     * operation.
     */
    public void start() {
        errorCount = 0;
        final ReadBlockTask readBlockTask = new ReadBlockTask(inputStream, outputStream);
        // poll the queue
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!paused) {
                AbstractSerialAccessTask task;
                // check for the next task to execute
                if (!queue.isEmpty()) {
                    task = queue.poll();
                } else {
                    // as default: execute the read task
                    task = readBlockTask;
                    task.setReceivers(receivers);
                }
                try {
                    if (serialTaskExecutor.submit(task).get()) {
                        errorCount = 0;
                    } else {
                        errorCount++;
                        if (errorCount >= MAX_ERROR_COUNT) {
                            log.warn("close channel by reaching error count: " + errorCount);
                            shutdownNow();
                        }
                    }
                } catch (InterruptedException e) {
                    log.error("serial access interrupted", e);
                    shutdownNow();
                } catch (ExecutionException e) {
                    log.error("execution error of serial access", e);
                    shutdownNow();
                }
            }
        }, 200, DELAY_IN_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Pause the running channel.
     */
    public void pause() {
        paused = true;
        log.info("bus data channel paused");
    }

    /**
     * Resume the paused channel.
     */
    public void resume() {
        if (paused) {
            paused = false;
            log.info("bus data channel resumed");
        } else {
            log.warn("can't resume bus data channel, it's not paused!");
        }
    }

    /**
     * Send the given {@link net.wbz.selectrix4java.data.BusData} to the output of the device. This call is
     * asynchronously executed from the queue.
     *
     * @param busData {@link net.wbz.selectrix4java.data.BusData} to send
     */
    public void send(BusData busData) {
        queue.offer(new WriteTask(inputStream, outputStream, busData));
    }

    /**
     * Send the given byte array to the output of the device. This call is asynchronously executed from the queue.
     *
     * @param data bytes to send
     */
    public void send(byte[] data) {
        queue.offer(new WriteTask(inputStream, outputStream, data));
    }

    /**
     * Stop the asnyc executions.
     */
    public void shutdownNow() {
        serialTaskExecutor.shutdownNow();
        scheduledExecutorService.shutdownNow();
        if (callback != null) {
            callback.channelClosed();
        }
    }

    public void setCallback(ChannelStateCallback callback) {
        this.callback = callback;
    }

    public void addBusDataReceiver(BusDataRecorder busDataRecorder) {
        receivers.add(busDataRecorder);
    }

    public void removeBusDataReceiver(BusDataRecorder busDataRecorder) {
        receivers.remove(busDataRecorder);
    }

    public boolean containsBusDataReceiver(BusDataRecorder busDataRecorder) {
        return receivers.contains(busDataRecorder);
    }

    protected List<BusDataReceiver> getReceivers() {
        return receivers;
    }

    public interface ChannelStateCallback {

        void channelClosed();
    }
}
