package net.wbz.selectrix4java.data;

import net.wbz.selectrix4java.bus.BusDataReceiver;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Daniel Tuerk
 */
abstract public class AbstractSerialAccessTask<T> implements Callable<T> {

    private final InputStream inputStream;
    private final OutputStream outputStream;
    private List<BusDataReceiver> receivers;

    public AbstractSerialAccessTask(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    protected InputStream getInputStream() {
        return inputStream;
    }

    protected OutputStream getOutputStream() {
        return outputStream;
    }

    public void setReceivers(List<BusDataReceiver> receivers) {
        this.receivers = receivers;
    }

    protected List<BusDataReceiver> getReceivers() {
        return receivers;
    }
}
