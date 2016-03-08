package net.wbz.selectrix4java.bus;

import java.math.BigInteger;
import java.util.Arrays;

import net.wbz.selectrix4java.bus.consumption.AbstractBusDataConsumer;
import net.wbz.selectrix4java.bus.consumption.AllBusDataConsumer;
import net.wbz.selectrix4java.bus.consumption.BusAddressData;
import net.wbz.selectrix4java.bus.consumption.BusAddressDataConsumer;
import net.wbz.selectrix4java.bus.consumption.BusBitConsumer;
import net.wbz.selectrix4java.bus.consumption.BusMultiAddressDataConsumer;
import net.wbz.selectrix4java.data.ReadBlockTask;
import net.wbz.selectrix4java.device.DeviceAccessException;
import net.wbz.selectrix4java.device.serial.BaseTest;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

/**
 * Test to check all {@link AbstractBusDataConsumer} implementations. Check
 * received values and number of calls. Consumers should be called at timestamp
 * of registration and for each changes of the corresponding data.
 * TODO: test for double registration -> single "first call" events
 *
 * @author Daniel Tuerk
 */
public class ConsumerTest extends BaseTest {

    @Test
    public void testAllBusDataConsumer() throws DeviceAccessException, InterruptedException {
        final int[] amountOfOverallEvents = { 0 };
        final TestDataSet testDataSet = new TestDataSet(1, 2, 10);
        getDevice().getBusDataDispatcher().registerConsumer(new AllBusDataConsumer() {
            @Override
            public void valueChanged(int bus, int address, int oldValue, int newValue) {
                printData(oldValue, newValue, bus, address);
                if (bus == testDataSet.getSendBus() && address == testDataSet.getSendAddress()) {
                    testDataSet.setResult(bus, address, newValue);
                }
                amountOfOverallEvents[0]++;
            }
        });
        sendAndAssertEventReceived(testDataSet);
        assertEventReceived(testDataSet, 2);

        Thread.sleep(500L);
        Assert.assertEquals("amount of overall event wrong", ReadBlockTask.LENGTH_OF_DATA_REPLY + 1,
                amountOfOverallEvents[0]);
    }

    @Test
    public void testBusAddressDataConsumer() throws DeviceAccessException, InterruptedException {
        final TestDataSet testDataSet = new TestDataSet(1, 2, 10);
        getDevice().getBusDataDispatcher()
                .registerConsumer(new BusAddressDataConsumer(testDataSet.getSendBus(), testDataSet.getSendAddress()) {
                    @Override
                    public void valueChanged(int oldValue, int newValue) {
                        printData(oldValue, newValue, getBus(), getAddress());
                        testDataSet.setResult(getBus(), getAddress(), newValue);
                    }
                });
        sendTestData(testDataSet);
        assertEventReceived(testDataSet, 2);
    }

    @Test
    public void testBusBitConsumer() throws DeviceAccessException, InterruptedException {
        final TestDataSet testDataSet = new TestDataSet(1, 2, 2);
        getDevice().getBusDataDispatcher().registerConsumer(
                new BusBitConsumer(testDataSet.getSendBus(), testDataSet.getSendAddress(), testDataSet.getSendValue()) {
                    @Override
                    public void valueChanged(int oldValue, int newValue) {
                        printData(oldValue, newValue, getBus(), getAddress());
                        testDataSet.setResult(getBus(), getAddress(), newValue == 1 ? 2 : 0);
                    }
                });
        sendTestData(testDataSet);
        assertEventReceived(testDataSet, 2);
    }

    @Test
    public void testBusMultiAddressDataConsumer() throws DeviceAccessException, InterruptedException {
        final TestDataSet testDataSetAddress1 = new TestDataSet(1, 2, 10);
        final TestDataSet testDataSetAddress2 = new TestDataSet(1, 3, 11);
        final TestDataSet testDataSetAddress3 = new TestDataSet(1, 4, 12);

        int[] addresses = Ints.toArray(Lists.newArrayList(testDataSetAddress1.getSendAddress(),
                testDataSetAddress2.getSendAddress(), testDataSetAddress3.getSendAddress()));

        getDevice().getBusDataDispatcher()
                .registerConsumer(new BusMultiAddressDataConsumer(testDataSetAddress1.getSendBus(), addresses) {
                    @Override
                    public void valueChanged(BusAddressData[] data) {
                        print("multi data: " + Arrays.deepToString(data));
                        testDataSetAddress1.setResult(data[0].getBus(), data[0].getAddress(),
                                data[0].getNewDataValue());
                        testDataSetAddress2.setResult(data[1].getBus(), data[1].getAddress(),
                                data[1].getNewDataValue());
                        testDataSetAddress3.setResult(data[2].getBus(), data[2].getAddress(),
                                data[2].getNewDataValue());
                    }
                });
        sendTestData(testDataSetAddress1);
        assertEventReceived(testDataSetAddress1, 2);
        sendTestData(testDataSetAddress2);
        assertEventReceived(testDataSetAddress2, 3);
        sendTestData(testDataSetAddress3);
        assertEventReceived(testDataSetAddress3, 4);

    }

    @Test
    public void testAllTogether() throws DeviceAccessException, InterruptedException {
        final TestDataSet testDataSet = new TestDataSet(0, 12, 8);

        final TestDataSet busAddressDataResult = testDataSet.getClone();
        getDevice().getBusDataDispatcher()
                .registerConsumer(new BusAddressDataConsumer(testDataSet.getSendBus(), testDataSet.getSendAddress()) {
                    @Override
                    public void valueChanged(int oldValue, int newValue) {
                        printData(oldValue, newValue, getBus(), getAddress());
                        busAddressDataResult.setResult(getBus(), getAddress(), newValue);
                    }
                });
        final TestDataSet busAllBusDataResult = testDataSet.getClone();
        getDevice().getBusDataDispatcher().registerConsumer(new AllBusDataConsumer() {
            @Override
            public void valueChanged(int bus, int address, int oldValue, int newValue) {
                if (bus == busAllBusDataResult.getSendBus() && address == busAllBusDataResult.getSendAddress()) {
                    printData(oldValue, newValue, bus, address);
                    busAllBusDataResult.setResult(bus, address, newValue);
                }
            }
        });
        final TestDataSet busBitResult = testDataSet.getClone();
        final int lowestSetBit = BigInteger.valueOf(busBitResult.getSendValue()).getLowestSetBit() + 1;
        getDevice().getBusDataDispatcher().registerConsumer(
                new BusBitConsumer(testDataSet.getSendBus(), testDataSet.getSendAddress(), lowestSetBit) {
                    @Override
                    public void valueChanged(int oldValue, int newValue) {
                        printData(oldValue, newValue, getBus(), getAddress());
                        busBitResult.setResult(getBus(), getAddress(),
                                newValue == 1 ? BigInteger.ZERO.setBit(lowestSetBit - 1).intValue() : 0);
                    }
                });
        final TestDataSet busMultiAddressDataResult = testDataSet.getClone();
        getDevice().getBusDataDispatcher().registerConsumer(
                new BusMultiAddressDataConsumer(testDataSet.getSendBus(), new int[] { testDataSet.getSendAddress() }) {
                    @Override
                    public void valueChanged(BusAddressData[] data) {
                        print("multi data: " + Arrays.deepToString(data));
                        busMultiAddressDataResult.setResult(data[0].getBus(), data[0].getAddress(),
                                data[0].getNewDataValue());
                    }
                });

        sendTestData(testDataSet);

        assertEventReceived(busAddressDataResult, 2);
        assertEventReceived(busAllBusDataResult, 2);
        assertEventReceived(busBitResult, 2);
        assertEventReceived(busMultiAddressDataResult, 2);
    }

    private void sendAndAssertEventReceived(TestDataSet testDataSet)
            throws InterruptedException, DeviceAccessException {
        sendTestData(testDataSet);
    }

    protected void sendTestData(TestDataSet testDataSet) throws InterruptedException, DeviceAccessException {
        Thread.sleep(200L);
        getDevice().getBusAddress(testDataSet.getSendBus(), (byte) testDataSet.getSendAddress())
                .sendData((byte) testDataSet.getSendValue());
        Thread.sleep(300L);
    }

}
