package net.wbz.selectrix4java.data;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.wbz.selectrix4java.bus.BusDataReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Deque;
import java.util.concurrent.*;

/**
 * The channel communicate with the device to execute read and write operations.
 * Each operation is an {@link net.wbz.selectrix4java.data.AbstractSerialAccessTask}.
 * The tasks can be put into the queue to execute by calling {@link net.wbz.selectrix4java.data.BusDataChannel#send(BusData)}.
 * <p/>
 * The queue is polled each time to execute an task. If no task is present in the queue, the channel send the
 * {@link ReadBlockTask} to read the actual values from the SX bus.
 * <p/>
 * State changes of the values are published to the given {@link net.wbz.selectrix4java.bus.BusDataReceiver}.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class BusDataChannel {
    private static final Logger log = LoggerFactory.getLogger(BusDataChannel.class);
    public static final long DELAY = 55L;
//    public static final long DELAY = 500L;

    private final Deque<AbstractSerialAccessTask> queue = new LinkedBlockingDeque<>();
    private final ScheduledExecutorService scheduledExecutorService;
    private final ExecutorService serialTaskExecutor;

    private final OutputStream outputStream;
    private final InputStream inputStream;

    private final BusDataReceiver receiver;

    /**
     * Create an new channel for the given IO streams of the connected device.
     *
     * @param inputStream  opened {@link java.io.InputStream}
     * @param outputStream opened {@link java.io.OutputStream}
     * @param receiver     {@link net.wbz.selectrix4java.bus.BusDataReceiver} to receive the values of the read operations
     */
    public BusDataChannel(InputStream inputStream, OutputStream outputStream, BusDataReceiver receiver) {
        this.outputStream = outputStream;
        this.inputStream = inputStream;
        this.receiver = receiver;

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("serial-io-executor-%d").build();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);
        serialTaskExecutor = Executors.newSingleThreadExecutor(namedThreadFactory);
    }

    public void start() {
        final ReadBlockTask readBlockTask = new ReadBlockTask(inputStream, outputStream, receiver);
        // poll the queue
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                AbstractSerialAccessTask task;
                // check for the next task to execute
                if (!queue.isEmpty()) {
                    task = queue.poll();
                } else {
                    // as default: execute the read task
                    task = readBlockTask;
                }
                try {
                    serialTaskExecutor.submit(task).get();
                } catch (InterruptedException e) {
                    log.error("serial access interrupted", e);
                } catch (ExecutionException e) {
                    log.error("execution error of serial access", e);
                }
            }
        }, 0L, DELAY, TimeUnit.MILLISECONDS);
    }

    /**
     * Send the given {@link net.wbz.selectrix4java.data.BusData} to the output of the device.
     * This call is asynchronously executed from the queue.
     *
     * @param busData {@link net.wbz.selectrix4java.data.BusData} to send
     */
    public void send(BusData busData) {
        queue.push(new WriteTask(inputStream, outputStream, busData));
    }

    /**
     * Stop the asnyc executions.
     */
    public void shutdownNow() {
        serialTaskExecutor.shutdownNow();
        scheduledExecutorService.shutdownNow();
    }
}