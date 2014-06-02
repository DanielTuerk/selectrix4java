package net.wbz.selecttrix4java.api.data;

import net.wbz.selecttrix4java.api.bus.BusDataReceiver;
import net.wbz.selecttrix4java.api.data.AbstractSerialAccessTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class ReadBlockTask extends AbstractSerialAccessTask<Void> {
    private static final Logger log = LoggerFactory.getLogger(ReadBlockTask.class);

    private final BusDataReceiver receiver;

    public ReadBlockTask(InputStream inputStream, OutputStream outputStream, BusDataReceiver receiver) {
        super(inputStream, outputStream);
        this.receiver = receiver;
    }

    @Override
    public Void call() throws Exception {
        byte[] replyBus0 = new byte[226];
        readBlock(120, 3, replyBus0);
        // bus 0
        receiver.received(0, Arrays.copyOfRange(replyBus0, 0, 112));
        // bus 1
        receiver.received(1, Arrays.copyOfRange(replyBus0, 113, 225));
        return null;
    }

    private void readBlock(int address, int data, byte[] reply) throws IOException {

        getOutputStream().write(new byte[]{(byte) address, (byte) data});
        getOutputStream().flush();

        int length = getInputStream().read(reply);
        if (length != reply.length) {
            throw new RuntimeException("block length invalid ("+length+")");
        }

    }
}
