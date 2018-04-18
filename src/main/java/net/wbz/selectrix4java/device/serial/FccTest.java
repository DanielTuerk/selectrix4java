package net.wbz.selectrix4java.device.serial;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.wbz.selectrix4java.block.FeedbackBlockListener;
import net.wbz.selectrix4java.block.FeedbackBlockModule;
import net.wbz.selectrix4java.device.Device;
import net.wbz.selectrix4java.device.DeviceAccessException;
import net.wbz.selectrix4java.device.DeviceConnectionListener;

/**
 * Executable to connect to a FCC device.
 * First program argument must be the serial device port. (e.g. /dev/tty.usbserial-1423)
 *
 * @author Daniel Tuerk
 */
public class FccTest {

    private final Device serialDevice;
    private boolean running;

    public FccTest(Device device) throws DeviceAccessException, InterruptedException {
        this.serialDevice = device;
        this.serialDevice.addDeviceConnectionListener(new DeviceConnectionListener() {
            @Override
            public void connected(Device device) {
                try {
                    // addBlock(56, device);

                    addBlock(59, device);

                    startThred(serialDevice);
                    // initConsole(device);
                } catch (DeviceAccessException | InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void disconnected(Device device) {

            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serialDevice.connect();
                } catch (DeviceAccessException e) {
                    e.printStackTrace();
                }

            }
        }).start();
        running = true;
        while (running) {
            Thread.sleep(500L);
        }
    }

    private static void addBlock(final int blockAddress, Device device) throws DeviceAccessException {
        FeedbackBlockModule feedbackBlockModule = device.getFeedbackBlockModule((byte) blockAddress,
                (byte) (blockAddress + 2), (byte) (blockAddress + 1));
        feedbackBlockModule.addFeedbackBlockListener(new FeedbackBlockListener() {
            @Override
            public void trainEnterBlock(int blockNumber, int train, boolean forward) {
                print("Block (%d): %d - train %d (direction: %b) ->> enter", blockAddress, blockNumber, train, forward);
            }

            @Override
            public void trainLeaveBlock(int blockNumber, int train, boolean forward) {
                print("Block (%d): %d - train %d (direction: %b) <<- exit", blockAddress, blockNumber, train, forward);
            }

            @Override
            public void blockOccupied(int blockNr) {
                // print("FeedbackBlockModule :: blockOccupied - nr: %d", blockNr);
            }

            @Override
            public void blockFreed(int blockNr) {
                // print("FeedbackBlockModule :: blockFreed - nr: %d", blockNr);
            }
        });
    }

    public static void print(String msg, Object... args) {
        System.out.println(new SimpleDateFormat("hh:mm").format(new Date(System.currentTimeMillis())) + " " + String
                .format(msg, args));
    }

    public static void main(String[] args) throws InterruptedException, DeviceAccessException {
        new FccTest(new SerialDevice(args[0], SerialDevice.DEFAULT_BAUD_RATE_FCC));
    }

    private void startThred(final Device serialDevice) throws InterruptedException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    initConsole(serialDevice);
                } catch (DeviceAccessException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        System.out.println("ende");
    }

    private void initConsole(Device serialDevice) throws DeviceAccessException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Geben Sie etwas ein: \n");
        String line;
        try {
            // boolean running = true;
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
                        serialDevice.getBusAddress(Integer.parseInt(parts[0]),
                                (byte) Integer.parseInt(parts[1]))
                                .sendData((byte) Integer.parseInt(parts[2]));
                        break;
                }
            }
            serialDevice.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
