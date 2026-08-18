package io.github.danieltuerk.selectrix4java.data;

import io.github.danieltuerk.selectrix4java.bus.BusDataReceiver;
import io.github.danieltuerk.selectrix4java.device.serial.SerialPort;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Abstract task for the access to the {@link OutputStream} and {@link InputStream} id a {@link
 * io.github.danieltuerk.selectrix4java.device.Device}.
 *
 * @author Daniel Tuerk
 */
abstract class AbstractSerialAccessTask implements Callable<Boolean> {

    private final SerialPort serialPort;
    private List<BusDataReceiver> receivers;

    /**
     * Create task for given port.
     *
     * @param serialPort {@link SerialPort}
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
