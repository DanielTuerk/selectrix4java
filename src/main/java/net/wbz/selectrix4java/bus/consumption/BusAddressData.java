package net.wbz.selectrix4java.bus.consumption;

/**
 * Data holder for {@link net.wbz.selectrix4java.bus.consumption.BusMultiAddressDataConsumer}.
 *
 * @author Daniel Tuerk
 */
public class BusAddressData {

    private final int bus;
    private final int address;
    private final int oldDataValue;
    private final int newDataValue;

    public BusAddressData(int bus, int address, int oldDataValue, int newDataValue) {
        this.bus = bus;
        this.address = address;
        this.oldDataValue = oldDataValue;
        this.newDataValue = newDataValue;
    }

    public int getBus() {
        return bus;
    }

    public int getAddress() {
        return address;
    }

    public int getOldDataValue() {
        return oldDataValue;
    }

    public int getNewDataValue() {
        return newDataValue;
    }

    @Override
    public String toString() {
        return "BusAddressData{" + "bus=" + bus + ", address=" + address + ", oldDataValue=" + oldDataValue
                + ", newDataValue=" + newDataValue + '}';
    }
}
