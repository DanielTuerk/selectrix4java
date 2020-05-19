package net.wbz.selectrix4java.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Write {@link net.wbz.selectrix4java.data.BusData} to the {@link java.io.OutputStream} of the connected device.
 *
 * @author Daniel Tuerk
 */
public class WriteTask extends AbstractSerialAccessTask {

    private static final Logger log = LoggerFactory.getLogger(WriteTask.class);

    private final BusData busData;
    private final byte[] data;

    /**
     * Create new task for an execution
     *
     * @param inputStream {@link java.io.InputStream}
     * @param outputStream {@link java.io.OutputStream}
     * @param data bytes to send
     */
    public WriteTask(InputStream inputStream, OutputStream outputStream, byte[] data) {
        super(inputStream, outputStream);
        this.busData = null;
        this.data = data;
    }

    /**
     * Create new task for an execution
     *
     * @param inputStream {@link java.io.InputStream}
     * @param outputStream {@link java.io.OutputStream}
     * @param busData {@link net.wbz.selectrix4java.data.BusData}
     */
    public WriteTask(InputStream inputStream, OutputStream outputStream, BusData busData) {
        super(inputStream, outputStream);
        this.busData = busData;
        this.data = null;
    }

    @Override
    public Boolean call() {
        try {
            // write to output
            if (data == null && busData != null) {
                log.debug(String.format("write: bus=%d address=%d data=%d", busData.getBus(), busData.getAddress(),
                    busData.getData()));
                byte address = BigInteger.valueOf(busData.getAddress()).setBit(7).byteValue();

                getOutputStream().write(new byte[]{(byte) busData.getBus(), address, (byte) busData.getData()});

                getOutputStream().flush();

            } else if (data != null && busData == null) {
                throw new RuntimeException("wtf? why no address byte?");
            } else {
                throw new RuntimeException("invalid data to send! Only byte array or BusData are valid!");
            }

            // read write reply as one byte
            int reply;
            do {
                reply = getInputStream().read();
            } while (reply < 0);

            if (reply == 0) {
                log.debug("write successful!");
            } else {
                log.warn("write error reply: " + reply);
            }

        } catch (IOException e) {
            log.error("error writing data", e);
            return false;
        }
        return true;
    }
}
