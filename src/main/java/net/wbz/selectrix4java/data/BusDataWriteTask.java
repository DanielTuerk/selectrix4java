package net.wbz.selectrix4java.data;

import net.wbz.selectrix4java.jna.SerialPort;

import java.math.BigInteger;

/**
 * Write {@link BusData} to the {@link java.io.OutputStream} of the connected device.
 *
 * @author Daniel Tuerk
 */
public class BusDataWriteTask extends WriteTask {

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
    }

}