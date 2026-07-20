package io.github.danieltuerk.selectrix4java.device.serial;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.BooleanSupplier;
import io.github.danieltuerk.selectrix4java.bus.TestDataSet;
import io.github.danieltuerk.selectrix4java.device.Device;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

/**
 * Base test class to test the communication of a device. Connection for the device is established in {@see #setup} and
 * closed in {@see #tearDown}.
 *
 * @author Daniel Tuerk
 */
public class BaseTest {

    public final static String DEVICE_ID = "/dev/tty.usbserial-141";

    /**
     * Bus data is delivered asynchronously by a background poll cycle, whose timing depends on
     * the machine running the test. Assertions therefore poll for the expected outcome up to this
     * timeout instead of relying on a fixed sleep, which was flaky on slower/shared CI runners.
     */
    private static final long AWAIT_TIMEOUT_MS = 5000L;
    private static final long AWAIT_POLL_INTERVAL_MS = 50L;

    private final Connection connection;

    /**
     * Base test for the {@link io.github.danieltuerk.selectrix4java.device.DeviceManager.DEVICE_TYPE#TEST} device.
     */
    public BaseTest() {
        this(Connection.createTestDeviceConnection());
    }

    /**
     * Base test for the given {@link Connection}.
     *
     * @param connection {@link Connection}
     */
    public BaseTest(Connection connection) {
        this.connection = connection;
    }

    public static void printData(int oldValue, int newValue, int bus, int address) {
        System.out.printf("%d/%d = old: %s - new: %s%n", bus, address, toUnsignedInt((byte) oldValue),
                toUnsignedInt((byte) newValue));
    }

    public static void printData(String msg, int oldValue, int newValue, int bus, int address) {
        System.out.printf("%s - %d/%d = old: %s - new: %s%n", msg, bus, address, toUnsignedInt((byte) oldValue),
                toUnsignedInt((byte) newValue));
    }

    protected static int toUnsignedInt(byte b) {
        return ((int) b) & 0xFF;
    }

    public static void print(String msg, Object... args) {
        System.out.println(new SimpleDateFormat("hh:mm").format(new Date(System.currentTimeMillis())) + " " + String
                .format(msg, args));
    }

    public Device getDevice() {
        return connection.getDevice();
    }

    @Before
    public void setup() {
        Assert.assertTrue("no connection", connection.connect());
        // The first read cycle of BusDataChannel and this fixed wait both start counting from the
        // same "connected" moment with the same 200ms delay, so a plain Thread.sleep(200L) here raced
        // the background read against consumer registration below - depending on which won, a
        // consumer registering while the first cycle was mid-flight could miss one bus's initial
        // dump. Waiting for both buses to actually have data avoids the race entirely.
        awaitCondition(() -> hasBusData(0) && hasBusData(1));
    }

    private boolean hasBusData(int busNr) {
        try {
            getDevice().getBusDataDispatcher().getData(busNr);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @After
    public void tearDown() {
        Assert.assertTrue("can't disconnect", connection.disconnect());
    }

    protected void assertEventReceived(TestDataSet testDataSet) {
        assertEventReceived(testDataSet, -1);
    }

    protected void assertEventReceived(TestDataSet testDataSet, int expectedResultCount) {
        if (expectedResultCount == -1) {
            awaitCondition(() -> testDataSet.getResultCallCount() > 0);
            Assert.assertTrue("no event received", testDataSet.getResultCallCount() > 0);
        } else {
            awaitCondition(() -> testDataSet.getResultCallCount() >= expectedResultCount);
            Assert.assertEquals("amount of events wrong", expectedResultCount, testDataSet.getResultCallCount());
        }
        assertTestData(testDataSet.getSendBus(), testDataSet.getReceivedBus(), testDataSet.getSendAddress(),
                testDataSet.getReceivedAddress(), testDataSet.getSendValue(), testDataSet.getReceivedValue());
    }

    /**
     * Poll {@code condition} until it becomes true or {@link #AWAIT_TIMEOUT_MS} elapses, instead of
     * sleeping a fixed duration. Leaves the actual pass/fail decision to the caller's assertion so a
     * genuinely wrong result still fails clearly instead of being masked by the wait.
     */
    protected static void awaitCondition(BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(AWAIT_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    protected void assertTestData(int expectedBus, int receivedBus, int expectedAddress, int receivedAddress,
            int expectedValue, int receivedValue) {
        assertEquals(expectedBus, receivedBus);
        assertEquals(expectedAddress, receivedAddress);
        assertEquals(expectedValue, receivedValue);
    }
}
