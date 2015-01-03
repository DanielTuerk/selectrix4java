package net.wbz.selectrix4java.data;

import net.wbz.selectrix4java.bus.BusDataReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * This task read the bus 0 and 1 the hole time and delegate the result
 * to an {@link net.wbz.selectrix4java.bus.BusDataReceiver}.
 * <p/>
 * As {@link net.wbz.selectrix4java.data.AbstractSerialAccessTask} it will
 * be used by the {@link net.wbz.selectrix4java.data.BusDataChannel}
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class ReadBlockTask extends AbstractSerialAccessTask<Void> {

    private static final Logger log = LoggerFactory.getLogger(ReadBlockTask.class);

    private final BusDataReceiver receiver;

    private byte[] reply = new byte[226];

    /**
     * Create new task.
     *
     * @param inputStream  open {@link java.io.InputStream}
     * @param outputStream open {@link java.io.OutputStream}
     * @param receiver     {@link net.wbz.selectrix4java.bus.BusDataReceiver} as callback
     */
    public ReadBlockTask(InputStream inputStream, OutputStream outputStream, BusDataReceiver receiver) {
        super(inputStream, outputStream);
        this.receiver = receiver;
    }

    @Override
    public Void call() throws Exception {
        try {
            readBlock(120, 3, reply);
        } catch (IOException e) {
            log.error("can't read block", e);
            return null;
        }
        // bus 0
        receiver.received(0, Arrays.copyOfRange(reply, 0, 112));
        // bus 1
        receiver.received(1, Arrays.copyOfRange(reply, 113, 225));
        return null;
    }

    private void readBlock(int address, int data, byte[] reply) throws IOException {
        try {
            getOutputStream().write(new byte[]{(byte) address, (byte) data});
            getOutputStream().flush();
        } catch (IOException e) {
            throw new RuntimeException("can't write to output", e);
        }

        int length = getInputStream().read(reply);
        if (length != reply.length) {
            throw new IOException("block length invalid (" + length + ")");
        }

    }
}
