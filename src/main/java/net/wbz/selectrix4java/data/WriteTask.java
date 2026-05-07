package net.wbz.selectrix4java.data;

import net.wbz.selectrix4java.jna.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

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
     * @param serialPort {@link net.wbz.selectrix4java.jna.SerialPort}
     * @param data       bytes to send
     */
    public WriteTask(SerialPort serialPort, byte[] data) {
        super(serialPort);
        this.busData = null;
        this.data = data;
    }

    /**
     * Create new task for an execution
     *
     * @param serialPort {@link SerialPort}
     * @param busData    {@link net.wbz.selectrix4java.data.BusData}
     */
    public WriteTask(SerialPort serialPort, BusData busData) {
        super(serialPort);
        this.busData = busData;
        this.data = null;
    }

    @Override
    public Boolean call() {
        // write to output
        if (data == null && busData != null) {
            log.debug("write: bus={} address={} data={}", busData.getBus(), busData.getAddress(), busData.getData());
            byte address = BigInteger.valueOf(busData.getAddress()).setBit(7).byteValue();

            getSerialPort().write(new byte[]{(byte) busData.getBus(), address, (byte) busData.getData()});
        } else if (data != null && busData == null) {
            throw new RuntimeException("wtf? why no address byte?");
        } else {
            throw new RuntimeException("invalid data to send! Only byte array or BusData are valid!");
        }


        byte[] buf = new byte[256];
        int reply = getSerialPort().read(buf, 1000);

        if (reply == 1) {
            log.debug("write successful!");
        } else {
            log.warn("write error reply: {}", reply);
        }

        return true;
    }
}
