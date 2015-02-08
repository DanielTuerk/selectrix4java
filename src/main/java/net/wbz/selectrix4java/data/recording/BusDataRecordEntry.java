package net.wbz.selectrix4java.data.recording;

/**
 * Model to store the bus data which is recorded by the {@link BusDataRecorder}
 * and played by {@link net.wbz.selectrix4java.device.test.BusDataPlayer}.
 *
 * @author Daniel Tuerk
 */
public class BusDataRecordEntry {

    private final long timestamp;
    private final int bus;
    private final byte[] data;

    public BusDataRecordEntry(long timestamp, int bus, byte[] data) {
        this.timestamp = timestamp;
        this.bus = bus;
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getBus() {
        return bus;
    }

    public byte[] getData() {
        return data;
    }
}
