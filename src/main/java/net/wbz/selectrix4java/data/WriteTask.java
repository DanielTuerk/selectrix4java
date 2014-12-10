package net.wbz.selectrix4java.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

/**
 * Write {@link net.wbz.selectrix4java.data.BusData} to the {@link java.io.OutputStream} of the
 * connected device.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class WriteTask extends AbstractSerialAccessTask<Boolean> {
    private static final Logger log = LoggerFactory.getLogger(WriteTask.class);

    private final BusData busData;

    /**
     * Create new task for an execution
     *
     * @param inputStream  {@link java.io.InputStream}
     * @param outputStream {@link java.io.OutputStream}
     * @param busData      {@link net.wbz.selectrix4java.data.BusData}
     */
    public WriteTask(InputStream inputStream, OutputStream outputStream, BusData busData) {
        super(inputStream, outputStream);
        this.busData = busData;
    }

    @Override
    public Boolean call() throws Exception {
        byte address;
        address = BigInteger.valueOf(busData.getAddress()).setBit(7).byteValue();
        getOutputStream().write(new byte[]{(byte) busData.getBus(), address, (byte) busData.getData()});
        getOutputStream().flush();
        log.debug("write reply: " + getInputStream().read());

        // TODO: fix this bullshit: do the fake read to avoid invalid reply
        Thread.sleep(BusDataChannel.DELAY);

        getOutputStream().write(new byte[]{(byte) 120, (byte) 3});
        getOutputStream().flush();

        byte[] busReadReply = new byte[226];
        int length = getInputStream().read(busReadReply);
        if (length != busReadReply.length) {
            log.error("block length invalid (" + length + ")");
        }

        return null;
    }
}
