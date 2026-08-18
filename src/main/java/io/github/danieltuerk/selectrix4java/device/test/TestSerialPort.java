package io.github.danieltuerk.selectrix4java.device.test;

import io.github.danieltuerk.selectrix4java.device.serial.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Test implementation which stores the written values into a byte array and read the byte array. Simulates a SX1 bus.
 *
 * @author Daniel Tuerk
 */
class TestSerialPort implements SerialPort {

    private static final Logger LOG = LoggerFactory.getLogger(TestSerialPort.class);
    /**
     * Container for the bus 0 and bus 1 for 113 addresses.
     */
    private final transient byte[] busData = new byte[226];

    TestSerialPort() {

    }


    private int toUnsignedInt(byte b) {
        return ((int) b) & 0xFF;
    }

    @Override
    public void write(byte[] data) {
        if (data.length == 3) {
            // write address value
            int address = (toUnsignedInt(data[0]) * 113) + (data[1] < 0 ? data[1] + 128 : data[1]);
            if (address >= busData.length) {
                LOG.debug("ignore address {} for test bus (max :{})", address, busData.length);
            } else {
                busData[address] = data[2];
            }
        }
    }

    @Override
    public int read(byte[] buffer, int timeoutMs) {
        if (buffer.length == busData.length) {
            System.arraycopy(busData, 0, buffer, 0, busData.length);
        } else {
            Arrays.fill(buffer, (byte) 0x00);
        }
        return buffer.length;
    }

    @Override
    public int readNonBlocking(byte[] buffer) {
        return 0;
    }

    @Override
    public int available() {
        return busData.length;
    }

    @Override
    public void close() {

    }
}
