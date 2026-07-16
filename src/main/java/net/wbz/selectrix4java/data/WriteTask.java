package net.wbz.selectrix4java.data;

import net.wbz.selectrix4java.jna.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Write data to the {@link java.io.OutputStream} of the connected device.
 *
 * @author Daniel Tuerk
 */
public class WriteTask extends AbstractSerialAccessTask {

    private static final Logger log = LoggerFactory.getLogger(WriteTask.class);

    private final byte[] data;
    private final byte[] expectedAnswer;

    /**
     * Create new task for an execution
     *
     * @param serialPort {@link net.wbz.selectrix4java.jna.SerialPort}
     * @param data       bytes to send
     */
    public WriteTask(SerialPort serialPort, byte[] data, byte[] expectedAnswer) {
        super(serialPort);
        this.expectedAnswer = expectedAnswer;
        this.data = data;
    }

    @Override
    public Boolean call() {
        if (data == null) {
            log.error("invalid data to send!");
            return false;
        }
        log.debug("write: {}", data);
        getSerialPort().write(data);

        var buf = new byte[expectedAnswer.length];
        int reply = getSerialPort().read(buf, 250);
        if (reply == expectedAnswer.length && Arrays.equals(buf, expectedAnswer)) {
            log.debug("write successful!");
        } else {
            log.warn("write error reply: {} ({})", reply, buf);
        }

        return true;
    }
}