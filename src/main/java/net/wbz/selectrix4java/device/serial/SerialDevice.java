package net.wbz.selectrix4java.device.serial;

import com.fazecast.jSerialComm.SerialPort;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import net.wbz.selectrix4java.bus.BusDataDispatcher;
import net.wbz.selectrix4java.data.BusDataChannel;
import net.wbz.selectrix4java.device.AbstractDevice;
import net.wbz.selectrix4java.device.DeviceAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link net.wbz.selectrix4java.device.Device} implementation for serial access like COM or USB.
 * Usage of the RXTX library. In the lib path of the JRE must be present the DLL or SO file for the native access.
 *
 * @author Daniel Tuerk
 */
public class SerialDevice extends AbstractDevice {

    private static final Logger log = LoggerFactory.getLogger(SerialDevice.class);

    /**
     * Default baud rate for the FCC.
     */
    public static final int DEFAULT_BAUD_RATE_FCC = 230400;

    /**
     * Default baud rate for the Stärz Interface.
     */
    public static final int DEFAULT_BAUD_RATE_STAERZ_INTERFACE = 19200;

    /**
     * I/O write access of the connected bus.
     */
    private OutputStream outputStream = null;

    /**
     * I/O read access of the connected bus.
     */
    private InputStream inputStream = null;

    /**
     * OS port for the device to handle the I/O operations.
     */
    private SerialPort serialPort = null;

    /**
     * Id of the device in the OS. (e.g. COM4)
     */
    private final String deviceId;

    /**
     * Baud rate of the device.
     */
    private final int baudRate;

    /**
     * Create device to connect to an serial interface.
     *
     * @param deviceId {@link java.lang.String} OS device id
     * @param baudRate {@link int} baud rate of the device
     */
    public SerialDevice(String deviceId, int baudRate) {
        this.deviceId = deviceId;
        this.baudRate = baudRate;
    }

    @Override
    public String getDeviceId() {
        return deviceId;
    }

    @Override
    protected BusDataChannel doConnect(BusDataDispatcher busDataDispatcher) throws DeviceAccessException {
        try {
            serialPort = SerialPort.getCommPort(deviceId);
            serialPort.setBaudRate(baudRate);
            serialPort.setNumDataBits(8);
            serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
            serialPort.setParity(SerialPort.NO_PARITY);
            serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);

            if (!serialPort.openPort()) {
                throw new DeviceAccessException("Unable to open serial port: %s".formatted(deviceId));
            }

            outputStream = serialPort.getOutputStream();
            inputStream = serialPort.getInputStream();

            return new BusDataChannel(inputStream, outputStream, busDataDispatcher);
        } catch (Exception e) {
            throw new DeviceAccessException("can't connect to device for id %s".formatted(deviceId), e);
        }
    }

    /**
     * @see net.wbz.selectrix4java.device.Device#disconnect()
     */
    @Override
    public void doDisconnect() {
        if (serialPort != null) {
            try {
                serialPort.getOutputStream().close();
            } catch (IOException e) {
                log.error("can't close output stream", e);
            }
            try {
                serialPort.getInputStream().close();
            } catch (IOException e) {
                log.error("can't close input stream", e);
            }
            outputStream = null;
            inputStream = null;
            serialPort.removeDataListener();
            serialPort.closePort();
        }
    }

    @Override
    public boolean isConnected() {
        return outputStream != null && inputStream != null;
    }

    /**
     * Test main method to send commands by console and print the output.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        SerialDevice serialDevice = new SerialDevice("/dev/tty.usbserial-145", SerialDevice.DEFAULT_BAUD_RATE_FCC);
        try {
            serialDevice.connect();

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Geben Sie etwas ein: ");
            String line;
            try {
                boolean running = true;
                while (running) {
                    line = console.readLine();
                    switch (line) {
                        case "exit":
                            running = false;
                            break;
                        case "fcc 1":
                            serialDevice.setRailVoltage(true);
                            break;
                        case "fcc 0":
                            serialDevice.setRailVoltage(false);
                            break;
                        default:
                            String[] parts = line.split(" ");
                            serialDevice.getBusAddress(Integer.parseInt(parts[0]), (byte) Integer.parseInt(parts[1]))
                                    .sendData((byte) Integer.parseInt(parts[2]));
                            break;
                    }
                }
                serialDevice.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (DeviceAccessException e) {
            e.printStackTrace();
        }
    }
}
