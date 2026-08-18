package io.github.danieltuerk.selectrix4java.bus.consumption;

/**
 * Consumer for a single bit of an address on a bus.
 *
 * @author Daniel Tuerk
 */
abstract public class BusBitConsumer extends BusAddressDataConsumer {

    private final int bit;

    /**
     * Create consumer for given bus, address and bit.
     *
     * @param bus     bus number
     * @param address address of bus
     * @param bit     bit number (1-8)
     */
    public BusBitConsumer(int bus, int address, int bit) {
        super(bus, address);
        this.bit = bit;
    }

    /**
     * Observed bit number.
     *
     * @return bit number
     */
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
