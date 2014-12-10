package net.wbz.selectrix4java.device.serial;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import net.wbz.selectrix4java.bus.BusDataDispatcher;
import net.wbz.selectrix4java.data.BusDataChannel;
import net.wbz.selectrix4java.device.AbstractDevice;
import net.wbz.selectrix4java.device.DeviceAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Enumeration;

/**
 * {@link net.wbz.selectrix4java.device.Device} implementation for serial access like COM or USB.
 * <p/>
 * Usage of the RXTX library. In the lib path of the JRE must be present the DLL or SO file for the native access.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
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
    public BusDataChannel doConnect(BusDataDispatcher busDataDispatcher) throws DeviceAccessException {
        System.setProperty("gnu.io.rxtx.SerialPorts", deviceId);
        Enumeration portList = CommPortIdentifier.getPortIdentifiers();
        if (portList.hasMoreElements()) {
            CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
            try {
                serialPort = (SerialPort) portId.open("net.wbz.selectrix4java", 2000);
                outputStream = serialPort.getOutputStream();
                inputStream = serialPort.getInputStream();
                serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
                serialPort.setSerialPortParams(baudRate,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);
            } catch (Exception e) {
                throw new DeviceAccessException(String.format("can't connect to device for id %s", deviceId));
            }
            return new BusDataChannel(inputStream, outputStream, busDataDispatcher);
        }
        throw new DeviceAccessException(String.format("no device found for id %s", deviceId));
    }

    /**
     * @see net.wbz.selectrix4java.device.Device#disconnect()
     * <p/>
     * Note: RXTX will crash on OSX (Linux and Windows is working)
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
            serialPort.removeEventListener();
            serialPort.close();
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
                    if (line.equals("exit")) {
                        running = false;
                    } else if (line.equals("fcc 1")) {
                        serialDevice.setRailVoltage(true);
                    } else if (line.equals("fcc 0")) {
                        serialDevice.setRailVoltage(false);
                    } else {
                        String[] parts = line.split(" ");
                        serialDevice.getBusAddress(Integer.parseInt(parts[0]),
                                (byte) Integer.parseInt(parts[1]))
                                .sendData((byte) Integer.parseInt(parts[2]));
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
