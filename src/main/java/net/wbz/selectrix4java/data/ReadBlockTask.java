package net.wbz.selectrix4java.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import net.wbz.selectrix4java.bus.BusDataReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task read the bus 0 and 1 the hole timestamp and delegate the result to the {@link
 * net.wbz.selectrix4java.bus.BusDataReceiver}s. As {@link net.wbz.selectrix4java.data.AbstractSerialAccessTask} it will
 * be used by the {@link net.wbz.selectrix4java.data.BusDataChannel}. TODO it's a FCC specific implementation
 *
 * @author Daniel Tuerk
 */
public class ReadBlockTask extends AbstractSerialAccessTask<Void> {

    /**
     * TODO only in SX1?
     */
    public static final int LENGTH_OF_DATA_REPLY = 226;
    private static final Logger log = LoggerFactory.getLogger(ReadBlockTask.class);
    private static final long TIMEOUT = 5000L;
    /**
     * TODO FCC specific
     */
    private static final int ADDRESS = 120;
    /**
     * TODO FCC specific
     */
    private static final int DATA = 3;

    private final byte[] reply = new byte[LENGTH_OF_DATA_REPLY];

    /**
     * Create new task.
     *
     * @param inputStream open {@link java.io.InputStream}
     * @param outputStream open {@link java.io.OutputStream}
     */
    ReadBlockTask(InputStream inputStream, OutputStream outputStream) {
        super(inputStream, outputStream);
    }

    @Override
    public Void call() {
        try {
            readBlock(reply);
        } catch (IOException e) {
            log.error("can't read block", e);
            return null;
        }
        for (final BusDataReceiver receiver : getReceivers()) {
            // bus 0
            receiver.received(0, Arrays.copyOfRange(reply, 0, 113));
            // bus 1
            receiver.received(1, Arrays.copyOfRange(reply, 113, 226));
        }
        return null;
    }

    private void readBlock(byte[] reply) throws IOException {
        // request bus data
        try {
            getOutputStream().write(new byte[]{(byte) ADDRESS, (byte) DATA});
            getOutputStream().flush();
        } catch (IOException e) {
            throw new RuntimeException("can't write to output", e);
        }

        // waiting for full response from FCC
        long maxWaitingTime = System.currentTimeMillis() + TIMEOUT;
        while (getInputStream().available() < LENGTH_OF_DATA_REPLY) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (System.currentTimeMillis() > maxWaitingTime) {
                break;
            }
        }

        // read response
        int length = getInputStream().read(reply);
        if (length != reply.length) {
            throw new IOException("block length invalid (" + length + ")");
        }
    }
}
