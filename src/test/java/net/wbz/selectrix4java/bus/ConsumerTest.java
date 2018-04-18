package net.wbz.selectrix4java.bus;

import java.math.BigInteger;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Iterables;

import net.wbz.selectrix4java.bus.consumption.AbstractBusDataConsumer;
import net.wbz.selectrix4java.bus.consumption.AllBusDataConsumer;
import net.wbz.selectrix4java.bus.consumption.BusAddressData;
import net.wbz.selectrix4java.bus.consumption.BusAddressDataConsumer;
import net.wbz.selectrix4java.bus.consumption.BusBitConsumer;
import net.wbz.selectrix4java.bus.consumption.BusMultiAddressDataConsumer;
import net.wbz.selectrix4java.data.ReadBlockTask;
import net.wbz.selectrix4java.device.DeviceAccessException;
import net.wbz.selectrix4java.device.serial.BaseTest;

/**
 * Test to check all {@link AbstractBusDataConsumer} implementations. Check
 * received values and number of calls. Consumers should be called at timestamp
 * of registration and for each changes of the corresponding data.
 * TODO: test for double registration -> single "first call" events
 *
 * @author Daniel Tuerk
 */
public class ConsumerTest extends BaseTest {

    @Override
    public void setup() throws InterruptedException {
        super.setup();
        getDevice().getBusDataDispatcher().reset();
    }

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
        // - 2 for the ignored address 111 on bus 0 and 1
        Assert.assertEquals("amount of overall event wrong", ReadBlockTask.LENGTH_OF_DATA_REPLY + 1 - (2),
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

        getDevice().getBusDataDispatcher()
                .registerConsumer(new BusMultiAddressDataConsumer(testDataSetAddress1.getSendBus(), testDataSetAddress1
                        .getSendAddress(), testDataSetAddress2.getSendAddress(), testDataSetAddress3.getSendAddress()) {
                    @Override
                    public void valueChanged(Collection<BusAddressData> data) {
                        print("multi data: " + Iterables.toString(data));
                        for (BusAddressData busAddressData : data) {
                            if (busAddressData.getAddress() == testDataSetAddress1.getSendAddress()) {
                                testDataSetAddress1.setResult(busAddressData.getBus(), busAddressData.getAddress(),
                                        busAddressData.getNewDataValue());
                            } else if (busAddressData.getAddress() == testDataSetAddress2.getSendAddress()) {
                                testDataSetAddress2.setResult(busAddressData.getBus(), busAddressData.getAddress(),
                                        busAddressData.getNewDataValue());
                            } else if (busAddressData.getAddress() == testDataSetAddress3.getSendAddress()) {
                                testDataSetAddress3.setResult(busAddressData.getBus(), busAddressData.getAddress(),
                                        busAddressData.getNewDataValue());
                            }
                        }
                    }
                });
        // expect one change of each address and initial state
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
                new BusMultiAddressDataConsumer(testDataSet.getSendBus(), testDataSet.getSendAddress()) {
                    @Override
                    public void valueChanged(Collection<BusAddressData> data) {
                        print("multi data: " + Iterables.toString(data));
                        BusAddressData busAddressData = data.iterator().next();
                        busMultiAddressDataResult.setResult(busAddressData.getBus(), busAddressData.getAddress(),
                                busAddressData.getNewDataValue());
                    }
                });

        sendTestData(testDataSet);

        assertEventReceived(busAddressDataResult, 1);
        assertEventReceived(busAllBusDataResult, 1);
        assertEventReceived(busBitResult, 1);
        assertEventReceived(busMultiAddressDataResult, 1);
    }

    private void sendAndAssertEventReceived(TestDataSet testDataSet)
            throws InterruptedException, DeviceAccessException {
        sendTestData(testDataSet);
    }

    protected void sendTestData(TestDataSet testDataSet) throws InterruptedException, DeviceAccessException {
        Thread.sleep(200L);
        getDevice().getBusAddress(testDataSet.getSendBus(), (byte) testDataSet.getSendAddress())
                .sendData((byte) testDataSet.getSendValue());
        Thread.sleep(200L);
    }

}
