package net.wbz.selectrix4java.bus;

/**
 * Model for values to send and result to assert in tests.
 *
 * @author Daniel Tuerk
 */
public class TestDataSet {

    private final int sendBus;
    private final int sendAddress;
    private final int sendValue;

    private int receivedBus;
    private int receivedAddress;
    private int receivedValue;

    private transient int resultCallCount = 0;

    public TestDataSet(int sendBus, int sendAddress, int sendValue) {
        this.sendBus = sendBus;
        this.sendAddress = sendAddress;
        this.sendValue = sendValue;
    }

    public void setResult(int bus, int address, int value) {
        receivedBus = bus;
        receivedAddress = address;
        receivedValue = value;
        resultCallCount++;
    }

    public int getSendBus() {
        return sendBus;
    }

    public int getSendAddress() {
        return sendAddress;
    }

    public int getSendValue() {
        return sendValue;
    }

    public int getReceivedBus() {
        return receivedBus;
    }

    public int getReceivedAddress() {
        return receivedAddress;
    }

    public int getReceivedValue() {
        return receivedValue;
    }

    public int getResultCallCount() {
        return resultCallCount;
    }

    /**
     * Create a clone for the same send values.
     *
     * @return new {@link net.wbz.selectrix4java.bus.TestDataSet}
     */
    public TestDataSet getClone() {
        return new TestDataSet(sendBus, sendAddress, sendValue);
    }
}
