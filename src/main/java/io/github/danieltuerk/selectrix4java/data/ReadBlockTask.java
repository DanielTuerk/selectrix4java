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

        // read response
        int length = getSerialPort().read(reply, 1000);
        if (length > 0 && length != reply.length) {
            log.error("block length invalid ({})", length);
            return false;
        }
        return true;
    }
}
