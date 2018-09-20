package net.wbz.selectrix4java.data;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Callable;
import net.wbz.selectrix4java.bus.BusDataReceiver;

/**
 * Abstract task for the access to the {@link OutputStream} and {@link InputStream} id a {@link
 * net.wbz.selectrix4java.device.Device}.
 *
 * @author Daniel Tuerk
 */
abstract class AbstractSerialAccessTask<T> implements Callable<T> {

    private final InputStream inputStream;
    private final OutputStream outputStream;
    private List<BusDataReceiver> receivers;

    /**
     * Create task for given streams.
     *
     * @param inputStream {@link InputStream}
     * @param outputStream {@link OutputStream}
     */
    AbstractSerialAccessTask(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    protected InputStream getInputStream() {
        return inputStream;
    }

    protected OutputStream getOutputStream() {
        return outputStream;
    }

    protected List<BusDataReceiver> getReceivers() {
        return receivers;
    }

    public void setReceivers(List<BusDataReceiver> receivers) {
        this.receivers = receivers;
    }
}
