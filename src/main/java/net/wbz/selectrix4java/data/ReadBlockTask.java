package net.wbz.selectrix4java.data;

import net.wbz.selectrix4java.bus.BusDataReceiver;
import net.wbz.selectrix4java.jna.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

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
     * @param serialPort open {@link SerialPort}
     */
    ReadBlockTask(SerialPort serialPort) {
        super(serialPort);
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
        getSerialPort().write(new byte[]{(byte) ADDRESS, (byte) DATA});

        // waiting for full response from FCC
        // TODO verify with connectd FCC if this is really needed, because the FCC should reply immediately and the read method should wait for the data. Maybe only needed for the test implementation?
        long maxWaitingTime = System.currentTimeMillis() + CONNECTION_TIMEOUT;
        while (getSerialPort().available() < LENGTH_OF_DATA_REPLY) {
            try {
                Thread.sleep(SX_DELAY_IN_MILLIS);
            } catch (InterruptedException e) {
                log.error("error to wait for read delay, e");
                return false;
            }
            if (System.currentTimeMillis() > maxWaitingTime) {
                log.error("timeout reached to wait for bus data reply");
                break;
            }
        }
        // read response
        int length = getSerialPort().read(reply, 1000);
        // TODO der 0 check ist neu und war vorher nicht
        if (length > 0 && length != reply.length) {
            log.error("block length invalid ({})", length);
            return false;
        }
        return true;
    }
}
