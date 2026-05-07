package net.wbz.selectrix4java.data;

import net.wbz.selectrix4java.bus.BusDataReceiver;
import net.wbz.selectrix4java.jna.SerialPort;
import net.wbz.selectrix4java.jna.SerialPortImpl;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Abstract task for the access to the {@link OutputStream} and {@link InputStream} id a {@link
 * net.wbz.selectrix4java.device.Device}.
 *
 * @author Daniel Tuerk
 */
abstract class AbstractSerialAccessTask implements Callable<Boolean> {

    private final SerialPort serialPort;
    private List<BusDataReceiver> receivers;

    /**
     * Create task for given port.
     *
     * @param serialPort {@link SerialPortImpl}
     */
    AbstractSerialAccessTask(SerialPort serialPort) {
        this.serialPort = serialPort;
    }

    protected SerialPort getSerialPort() {
        return serialPort;
    }

    protected List<BusDataReceiver> getReceivers() {
        return receivers;
    }

    public void setReceivers(List<BusDataReceiver> receivers) {
        this.receivers = receivers;
    }
}
