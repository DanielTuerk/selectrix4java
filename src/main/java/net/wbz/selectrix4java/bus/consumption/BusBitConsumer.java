package net.wbz.selectrix4java.bus.consumption;

/**
 * @author Daniel Tuerk
 */
abstract public class BusBitConsumer extends BusAddressDataConsumer {

    private final int bit;

    public BusBitConsumer(int bus, int address, int bit) {
        super(bus, address);
        this.bit = bit;
    }

    public int getBit() {
        return bit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        BusBitConsumer that = (BusBitConsumer) o;

        return bit == that.bit;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + bit;
        return result;
    }
}
