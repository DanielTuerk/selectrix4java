package net.wbz.selectrix4java.device.test;

import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test implementation which stores the written values into an byte array and read the byte array. Simulates a SX1 bus.
 *
 * @author Daniel Tuerk
 */
class TestBus {

    private static final Logger LOG = LoggerFactory.getLogger(TestBus.class);
    private final InputStream inputStream;
    private final OutputStream outputStream;
    /**
     * Container for the bus 0 and bus 1 for 113 addresses.
     */
    private final transient byte[] busData = new byte[226];

    TestBus() {
        inputStream = new InputStream() {
            @Override
            public int read() {
                return 0;
            }

            @Override
            public int read(byte[] b) {
                System.arraycopy(busData, 0, b, 0, busData.length);
                return b.length;
            }

            @Override
            public int available() {
                return busData.length;
            }
        };
        outputStream = new OutputStream() {
            @Override
            public void write(int b) {
                throw new RuntimeException("not implemented");
            }

            @Override
            public void write(byte[] b) {
                if (b.length == 3) {
                    // write address value
                    int address = (toUnsignedInt(b[0]) * 113) + (b[1] < 0 ? b[1] + 128 : b[1]);
                    if (address >= busData.length) {
                        LOG.debug("ignore address " + address + " for test bus (max :" + busData.length + ")");
                    } else {
                        busData[address] = b[2];
                    }
                }
            }
        };

    }

    InputStream getInputStream() {
        return inputStream;
    }

    OutputStream getOutputStream() {
        return outputStream;
    }

    private int toUnsignedInt(byte b) {
        return ((int) b) & 0xFF;
    }
}
