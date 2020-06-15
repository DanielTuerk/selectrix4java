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
public class ReadBlockTask extends AbstractSerialAccessTask {

    /**
     * Length of the array of the SX 0 and 1 bus.
     */
    public static final int LENGTH_OF_DATA_REPLY = 226;
    private static final Logger log = LoggerFactory.getLogger(ReadBlockTask.class);
    /**
     * Timeout for read the bus.
     */
    private static final long CONNECTION_TIMEOUT = 5000L;
    /**
     * TODO FCC specific
     */
    private static final int ADDRESS = 120;
    /**
     * TODO FCC specific
     */
    private static final int DATA = 3;
    /**
     * Delay to read the SX bus.
     */
    public static final long SX_DELAY_IN_MILLIS = 77L;

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
    public Boolean call() {
        boolean valid = readBlock(reply);
        for (final BusDataReceiver receiver : getReceivers()) {
            // bus 0
            receiver.received(0, Arrays.copyOfRange(reply, 0, 113));
            // bus 1
            receiver.received(1, Arrays.copyOfRange(reply, 113, 226));
        }
        return valid;
    }

    private boolean readBlock(byte[] reply) {
        // request bus data
        try {
            getOutputStream().write(new byte[]{(byte) ADDRESS, (byte) DATA});
            getOutputStream().flush();

            // waiting for full response from FCC
            long maxWaitingTime = System.currentTimeMillis() + CONNECTION_TIMEOUT;
            while (getInputStream().available() < LENGTH_OF_DATA_REPLY) {
                try {
                    Thread.sleep(SX_DELAY_IN_MILLIS);
                } catch (InterruptedException e) {
                    log.error("error to wait for read delay, e");
                    return false;
                }
                if (System.currentTimeMillis() > maxWaitingTime) {
                    break;
                }
            }
            // read response
            int length = getInputStream().read(reply);
            if (length != reply.length) {
                log.error("block length invalid (" + length + ")");
                return false;
            }
        } catch (IOException e) {
            log.error("can't write to output", e);
            return false;
        }
        return true;
    }
}
