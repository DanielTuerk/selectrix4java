package net.wbz.selectrix4java.device.serial;

import junit.framework.Assert;
import net.wbz.selectrix4java.bus.BusAddressListener;
import net.wbz.selectrix4java.bus.TestDataSet;
import net.wbz.selectrix4java.device.Device;
import net.wbz.selectrix4java.device.DeviceAccessException;
import org.junit.After;
import org.junit.Before;

/**
 * @author Daniel Tuerk
 */
public class BaseTest {

    public final static String DEVICE_ID_TEST = "test";
    public final static String DEVICE_ID = "/dev/tty.usbserial-141";

    private final Connection connection;

    public BaseTest(Connection connection) {
        this.connection = connection;
    }

    public Device getDevice() {
        return connection.getDevice();
    }

    @Before
    public void setup() {
        Assert.assertTrue("no connection", connection.connect());
    }

    @After
    public void tearDown() {
        Assert.assertTrue("can't disconnect", connection.disconnect());
    }

    protected void addConsoleBusAddressListener(final int bus, final int address) throws DeviceAccessException {
        getDevice().getBusAddress(bus, (byte) address).addListener(new BusAddressListener() {
            @Override
            public void dataChanged(byte oldValue, byte newValue) {
                printData(oldValue, newValue, bus, address);
            }
        });
    }

    public static void printData(int oldValue, int newValue, int bus, int address) {
        System.out.printf("%d/%d = old: %s - new: %s%n", bus, address, toUnsignedInt((byte) oldValue), toUnsignedInt((byte) newValue));
    }

    protected static int toUnsignedInt(byte b) {
        return ((int) b) & 0xFF;
    }

    public static void print(String msg, Object... args) {
        System.out.println(String.format(msg, args));
    }


    protected void assertEventReceived(TestDataSet testDataSet) {
        assertEventReceived(testDataSet, -1);
    }

    protected void assertEventReceived(TestDataSet testDataSet, int expectedResultCount) {
        if (expectedResultCount == -1) {
            Assert.assertTrue("no event received", testDataSet.getResultCallCount() > 0);
        } else {
            Assert.assertEquals("amount of events wrong", expectedResultCount, testDataSet.getResultCallCount());
        }
        assertTestData(testDataSet.getSendBus(), testDataSet.getReceivedBus(),
                testDataSet.getSendAddress(), testDataSet.getReceivedAddress(),
                testDataSet.getSendValue(), testDataSet.getReceivedValue());
    }

    protected void assertTestData(int expectedBus, int receivedBus, int expectedAddress, int receivedAddress, int expectedValue, int receivedValue) {
        Assert.assertEquals(expectedBus, receivedBus);
        Assert.assertEquals(expectedAddress, receivedAddress);
        Assert.assertEquals(expectedValue, receivedValue);
    }
}
