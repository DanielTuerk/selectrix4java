package net.wbz.selectrix4java.device.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Test implementation which stores the written values into an byte array and read the byte array.
 *
 * Simulates a SX1 bus.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class TestBus {

    /**
     * Container for the bus 0 and bus 1 for 113 addresses.
     */
    private transient byte[] busData = new byte[226];

    private final InputStream inputStream;
    private final OutputStream outputStream;

    public TestBus() {
        inputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                return 0;
            }

            @Override
            public int read(byte[] b) throws IOException {
                System.arraycopy(busData, 0, b, 0, busData.length);
                return b.length;
            }

            @Override
            public int available() throws IOException {
                return busData.length;
            }
        };
        outputStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new RuntimeException("not implemented");
            }

            @Override
            public void write(byte[] b) throws IOException {
                if (b.length == 2) {
                    //write address value
//                    int address = (toUnsignedInt(b[0]) * 113) + (b[1] < 0 ? b[1] + 128 : b[1]);
                    busData[toUnsignedInt(b[0])] = b[1];
                }
            }
        };

    }

    private int toUnsignedInt(byte b) {
        return ((int) b) & 0xFF;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }
}
