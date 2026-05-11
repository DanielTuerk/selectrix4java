package net.wbz.selectrix4java.device.serial;

import net.wbz.selectrix4java.block.FeedbackBlockModule;
import net.wbz.selectrix4java.bus.BusAddress;
import net.wbz.selectrix4java.bus.BusAddressBitListener;
import net.wbz.selectrix4java.bus.BusAddressListener;
import net.wbz.selectrix4java.bus.BusDataDispatcher;
import net.wbz.selectrix4java.data.BusDataChannel;
import net.wbz.selectrix4java.device.AbstractDevice;
import net.wbz.selectrix4java.device.DeviceAccessException;
import net.wbz.selectrix4java.device.RailVoltageListener;
import net.wbz.selectrix4java.jna.Parity;
import net.wbz.selectrix4java.jna.SerialConfig;
import net.wbz.selectrix4java.jna.SerialPortImpl;
import net.wbz.selectrix4java.jna.SerialPortLister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * {@link net.wbz.selectrix4java.device.Device} implementation for serial access like COM or USB.
 *
 * @author Daniel Tuerk
 */
public class SerialDevice extends AbstractDevice {

    private static final Logger log = LoggerFactory.getLogger(SerialDevice.class);

    /**
     * Default address for the rail voltage.
     */
    private static final int RAILVOLTAGE_ADDRESS = 109;
    private static final int RAILVOLTAGE_BIT = 8;

    /**
     * Default baud rate for the FCC.
     */
    public static final int DEFAULT_BAUD_RATE_FCC = 230400;

    /**
     * Default baud rate for the Stärz Interface.
     */
    public static final int DEFAULT_BAUD_RATE_STAERZ_INTERFACE = 19200;

    /**
     * OS port for the device to handle the I/O operations.
     */
    private SerialPortImpl serialPort = null;

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
            serialPort = SerialPortImpl.open(deviceId,
                SerialConfig.builder()
                    .baudRate(baudRate)
                    .dataBits(8)
                    .stopBits(1)
                    .parity(Parity.NONE)
                    .build()
            );
            return new BusDataChannel(serialPort, busDataDispatcher);
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
            serialPort.close();
        }
    }

    @Override
    public boolean isConnected() {
        return serialPort != null;
    }

    @Override
    protected void initSystemFormatListener() throws DeviceAccessException {
        getBusAddress(1, (byte) 110).addListener(new BusAddressListener() {

            @Override
            public void dataChanged(byte oldValue, byte newValue) {

                var wrappedOldValue = BigInteger.valueOf(oldValue);
                var wrappedNewValue = BigInteger.valueOf(newValue);
                int oldSystemFormat = wrappedOldValue.clearBit(5).clearBit(6).clearBit(7).intValue() & 0xff;
                int newSystemFormat = wrappedNewValue.clearBit(5).clearBit(6).clearBit(7).intValue() & 0xff;

                if (oldSystemFormat != newSystemFormat) {
                    fireSystemFormat(convertSystemFormat(newSystemFormat));
                }
            }

            private void fireSystemFormat(final SYSTEM_FORMAT systemFormat) {
                getSystemFormatListeners().forEach(
                    listener -> listener.systemFormatChanged(systemFormat));
            }
        });
    }

    /**
     * Convert the given data value for bits 1-4 to the {@link net.wbz.selectrix4java.device.Device.SYSTEM_FORMAT}.
     *
     * @param systemFormat integer value of the system format
     * @return {@link net.wbz.selectrix4java.device.Device.SYSTEM_FORMAT}
     */
    private SYSTEM_FORMAT convertSystemFormat(int systemFormat) {
        return switch (systemFormat) {
            case 0 -> SYSTEM_FORMAT.ONLY_SX1;
            case 2 -> SYSTEM_FORMAT.SX1_SX2;
            case 4 -> SYSTEM_FORMAT.SX1_SX2_DCC;
            case 6 -> SYSTEM_FORMAT.ONLY_DCC;
            case 5 -> SYSTEM_FORMAT.SX1_SX2_MM;
            case 7 -> SYSTEM_FORMAT.ONLY_MM;
            case 11 -> SYSTEM_FORMAT.SX1_SX2_DCC_MM;
            default -> SYSTEM_FORMAT.UNKNOWN;
        };
    }

    @Override
    protected void initRailVoltageListener() throws DeviceAccessException {
        getRailVoltageAddress().addListener(new BusAddressBitListener(RAILVOLTAGE_BIT) {
            @Override
            public void bitChanged(boolean oldValue, boolean newValue) {
                if (newValue) {
                    railVoltageSwitchedOn();
                } else {
                    railVoltageSwitchedOff();
                }
                // inform listeners
                for (RailVoltageListener listener : getRailVoltageListeners()) {
                    listener.changed(newValue);
                }
            }
        });
    }

    /**
     * Handle turned on rail voltage.
     */
    private void railVoltageSwitchedOn() {
        for (FeedbackBlockModule feedbackBlockModule : getFeedbackBlockModules()) {
            feedbackBlockModule.requestNewFeedbackData();
        }
    }

    /**
     * Handle turned off rail voltage.
     */
    private void railVoltageSwitchedOff() {
        // reset FeedbackModules;
        getFeedbackBlockModules().forEach(FeedbackBlockModule::reset);
    }

    /**
     * Read the actual value of the rail voltage.
     *
     * @return {@link boolean} state
     */
    @Override
    public boolean getRailVoltage() throws DeviceAccessException {
        return BigInteger.valueOf(getRailVoltageAddress().getData()).testBit(RAILVOLTAGE_BIT);
    }

    /**
     * Change rail voltage.
     *
     * @param state {@link java.lang.Boolean} state
     */
    public void setRailVoltage(boolean state) throws DeviceAccessException {
        BusAddress busAddress = getBusAddress(1, (byte) 255);
        if (state) {
            busAddress.sendData((byte) 1);
        } else {
            busAddress.sendData((byte) 0);
        }
    }


    @Override
    public BusAddress getRailVoltageAddress() throws DeviceAccessException {
        return getBusAddress(1, (byte) RAILVOLTAGE_ADDRESS);
    }

    @Override
    public void switchDeviceSystemFormat() {
        sendNative(new byte[]{(byte) 131, (byte) 160, (byte) 0, (byte) 0, (byte) 0});
    }

    @Override
    public SYSTEM_FORMAT getActualSystemFormat() throws DeviceAccessException {
        BigInteger wrappedData = BigInteger.valueOf(getBusAddress(0, (byte) 110).getData());
        return convertSystemFormat(wrappedData.clearBit(5).clearBit(6).clearBit(7).intValue() & 0xff);
    }

    @Override
    public String toString() {
        return "SerialDevice{deviceId='%s', connected='%s'}".formatted(deviceId, isConnected());
    }

    /**
     * Test main method to send commands by console and print the output.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        List<SerialPortLister.PortInfo> list = SerialPortLister.list();
        var i = new AtomicInteger(1);
        log.info("available serial devices:\n{}",
            list.stream()
                .map(x -> String.format("%d\t%s\t%s", i.getAndIncrement(), x.path(), x.name()))
                .collect(Collectors.joining("\n")));


        Scanner scanner = new Scanner(System.in);

        log.info("Enter device ID (or number from list): ");
        var input = scanner.nextLine();

        String deviceId;
        try {
            int i1 = Integer.parseInt(input);
            deviceId = list.get(i1 - 1).path();
        } catch (NumberFormatException e) {
            deviceId = input;
        }

        log.info("connect to: {}", deviceId);
        SerialDevice serialDevice = new SerialDevice(deviceId, SerialDevice.DEFAULT_BAUD_RATE_FCC);
        try {
            serialDevice.connect();

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            log.info("Please enter command with <bus> <address> <value>: ");
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
                log.error("I/O error", e);
            }
        } catch (DeviceAccessException e) {
            log.error("device connection error", e);
        }
    }
}
