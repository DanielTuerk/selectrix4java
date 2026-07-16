package net.wbz.selectrix4java.data;

import net.wbz.selectrix4java.jna.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

/**
 * Write {@link BusData} to the {@link java.io.OutputStream} of the connected device.
 *
 * @author Daniel Tuerk
 */

public class BusDataWriteTask extends WriteTask {
    private static final Logger log = LoggerFactory.getLogger(BusDataWriteTask.class);

    private final BusData busData;
    /**
     * Create new task for an execution
     *
     * @param serialPort {@link SerialPort}
     * @param busData    {@link BusData}
     */
    public BusDataWriteTask(SerialPort serialPort, BusData busData) {
        super(serialPort,
                new byte[]{(byte) busData.getBus(),
                        BigInteger.valueOf(busData.getAddress()).setBit(7).byteValue(),
                        (byte) busData.getData()},
                new byte[]{0x00});
        this.busData = busData;
    }

    @Override
    public Boolean call() {
        log.debug("write BusData: {}", busData);
        return super.call();
    }
}