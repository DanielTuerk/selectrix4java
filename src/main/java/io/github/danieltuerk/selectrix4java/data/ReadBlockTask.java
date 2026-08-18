package io.github.danieltuerk.selectrix4java.data;

import io.github.danieltuerk.selectrix4java.bus.BusDataReceiver;
import io.github.danieltuerk.selectrix4java.device.serial.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * This task read the bus 0 and 1 the hole timestamp and delegate the result to the {@link
 * io.github.danieltuerk.selectrix4java.bus.BusDataReceiver}s. As {@link io.github.danieltuerk.selectrix4java.data.AbstractSerialAccessTask} it will
 * be used by the {@link io.github.danieltuerk.selectrix4java.data.BusDataChannel}. TODO it's a FCC specific implementation
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
     * Length of the data of a single bus in the reply.
     */
    private static final int LENGTH_OF_BUS_DATA = LENGTH_OF_DATA_REPLY / 2;
    /**
     * Address of the FCC multiplex counter which changes on every bus cycle. Excluded from the
     * change logging to avoid log spam.
     */
    private static final int MULTIPLEX_COUNTER_ADDRESS = 111;
    /**
     * Maximum number of address changes listed in a single change log line.
     */
    private static final int MAX_LOGGED_CHANGES = 10;
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
     * Previous valid reply to detect and log data changes. {@code null} until the first valid read.
     */
    private byte[] previousReply = null;
    /**
     * Whether the last read attempt returned a valid block. Used to log established/recovered
     * state transitions of the serial connection without spamming.
     */
    private boolean lastReadValid = false;

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
        if (!valid) {
            lastReadValid = false;
            // never dispatch a partial or stale buffer - the initial zero baseline is
            // established explicitly by BusDataChannel#start
            return false;
        }
        if (!lastReadValid) {
            lastReadValid = true;
            log.info("serial block read established ({} bytes)", LENGTH_OF_DATA_REPLY);
        }
        logDataChanges();

        for (final BusDataReceiver receiver : getReceivers()) {
            // bus 0
            receiver.received(0, Arrays.copyOfRange(reply, 0, LENGTH_OF_BUS_DATA));
            // bus 1
            receiver.received(1, Arrays.copyOfRange(reply, LENGTH_OF_BUS_DATA, LENGTH_OF_DATA_REPLY));
        }
        return true;
    }

    /**
     * Log a compact single line for the addresses whose data changed since the previous valid
     * block. The multiplex counter is excluded because it changes on every bus cycle. Helps to
     * attribute missing or unexpected events to the serial layer or the consumer layer.
     */
    private void logDataChanges() {
        if (!log.isDebugEnabled()) {
            return;
        }
        if (previousReply != null) {
            StringBuilder changes = new StringBuilder();
            int changeCount = 0;
            for (int i = 0; i < reply.length; i++) {
                int address = i % LENGTH_OF_BUS_DATA;
                if (address == MULTIPLEX_COUNTER_ADDRESS || reply[i] == previousReply[i]) {
                    continue;
                }
                changeCount++;
                if (changeCount <= MAX_LOGGED_CHANGES) {
                    if (!changes.isEmpty()) {
                        changes.append(", ");
                    }
                    changes.append("bus").append(i / LENGTH_OF_BUS_DATA).append('[').append(address).append("] ")
                        .append(previousReply[i] & 0xff).append("->").append(reply[i] & 0xff);
                }
            }
            if (changeCount > 0) {
                if (changeCount > MAX_LOGGED_CHANGES) {
                    changes.append(" (+").append(changeCount - MAX_LOGGED_CHANGES).append(" more)");
                }
                log.debug("serial data changed: {}", changes);
            }
        }
        previousReply = Arrays.copyOf(reply, reply.length);
    }

    private boolean readBlock(byte[] reply) {
        // request bus data
        getSerialPort().write(new byte[]{(byte) ADDRESS, (byte) DATA});

        // read response
        int length = getSerialPort().read(reply, 1000);
        if (length != reply.length) {
            log.error("block length invalid ({})", length);
            return false;
        }
        return true;
    }
}
