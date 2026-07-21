package io.github.danieltuerk.selectrix4java.runnables;

import io.github.danieltuerk.selectrix4java.block.FeedbackBlockListener;
import io.github.danieltuerk.selectrix4java.device.Device;
import io.github.danieltuerk.selectrix4java.device.DeviceAccessException;
import io.github.danieltuerk.selectrix4java.device.DeviceConnectionListener;
import io.github.danieltuerk.selectrix4java.device.serial.SerialDevice;
import io.github.danieltuerk.selectrix4java.train.TrainModule;
import io.github.danieltuerk.selectrix4java.train.TrainModule.DRIVING_DIRECTION;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main to add listener for value changes to address for connected serial device.
 *
 * @author Daniel Tuerk
 */
class MonitorAddressChangesConsoleMain {

    /**
     * Test main method to send commands by console and print the output.
     *
     * @param args ignored
     */
    public static void main(String[] args) {

        Path destinationFolder = Paths.get("");

        final SerialDevice serialDevice = new SerialDevice("/dev/tty.usbserial-141",
                SerialDevice.DEFAULT_BAUD_RATE_FCC);
        try {
            serialDevice.addDeviceConnectionListener(new DeviceConnectionListener() {
                @Override
                public void connected(Device device) {
                    System.out.println("connected");
                }

                @Override
                public void disconnected(Device device) {
                    System.out.println("disconnected");
                }
            });
            serialDevice.connect();

            addFeedbackModule(serialDevice, 50);
            addFeedbackModule(serialDevice, 53);

            TrainModule trainModuleA = serialDevice.getTrainModule(25); // VT 18.16
            trainModuleA.setLight(true);
            TrainModule trainModuleB = serialDevice.getTrainModule(20); // BR 183
            trainModuleB.setLight(true);

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Input: ");
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
                            serialDevice.startRecording(destinationFolder);
                            serialDevice.setRailVoltage(true);
                            break;
                        case "fcc 0":
                            if (serialDevice.isRecording()) {
                                serialDevice.stopRecording();
                                serialDevice.setRailVoltage(false);
                            }
                            break;
                        case "t1 f":
                            trainModuleA.setDirection(DRIVING_DIRECTION.FORWARD);
                            Thread.sleep(200L);
                            trainModuleA.setDrivingLevel(10);
                            break;
                        case "t1 b":
                            trainModuleA.setDirection(DRIVING_DIRECTION.BACKWARD);
                            Thread.sleep(200L);
                            trainModuleA.setDrivingLevel(10);
                            break;
                        case "t1 s":
                            trainModuleA.setDrivingLevel(0);
                            break;
                        case "t2 f":
                            trainModuleB.setDirection(DRIVING_DIRECTION.FORWARD);
                            Thread.sleep(200L);
                            trainModuleB.setDrivingLevel(10);
                            break;
                        case "t2 b":
                            trainModuleB.setDirection(DRIVING_DIRECTION.BACKWARD);
                            Thread.sleep(200L);
                            trainModuleB.setDrivingLevel(10);
                            break;
                        case "t2 s":
                            trainModuleB.setDrivingLevel(0);
                            break;
                        default:
                            try {
                                String[] parts = line.split(" ");
                                serialDevice
                                        .getBusAddress(Integer.parseInt(parts[0]), (byte) Integer.parseInt(parts[1]))
                                        .sendData((byte) Integer.parseInt(parts[2]));
                            } catch (Exception ignored) {

                            }
                            break;
                    }
                }
                serialDevice.disconnect();
            } catch (IOException | InterruptedException e) {
                System.out.println(e.getMessage());
            }
        } catch (DeviceAccessException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void addFeedbackModule(SerialDevice serialDevice, final int address) throws DeviceAccessException {
        serialDevice.getFeedbackBlockModule(address, address + 2, address + 1)
                .addFeedbackBlockListener(new FeedbackBlockListener() {
                    @Override
                    public void trainEnterBlock(int blockNumber, int trainAddress, boolean forward) {
                        System.out.println(
                                address + " ENTER " + blockNumber + ": " + trainAddress + " ( " + (forward ? "FORWARD"
                                        : "BACKWARD") + ")");
                    }

                    @Override
                    public void trainLeaveBlock(int blockNumber, int trainAddress, boolean forward) {
                        System.out.println(
                                address + " EXIT  " + blockNumber + ": " + trainAddress + " ( " + (forward ? "FORWARD"
                                        : "BACKWARD") + ")");
                    }

                    @Override
                    public void blockOccupied(int blockNr) {
                        System.out.println(address + ": block - " + blockNr + " occupied");
                    }

                    @Override
                    public void blockFreed(int blockNr) {
                        System.out.println(address + ": block - " + blockNr + " freed");
                    }
                });
    }

}
