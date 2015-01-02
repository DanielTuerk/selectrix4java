package net.wbz.selectrix4java.bus;

/**
 * @author Daniel Tuerk
 */
abstract public class BusBitConsumer extends BusDataConsumer {
    private int bit;
    public BusBitConsumer(int bus, int address, int bit) {
        super(bus, address);
        this.bit=bit;
    }

    public int getBit() {
        return bit;
    }
}
