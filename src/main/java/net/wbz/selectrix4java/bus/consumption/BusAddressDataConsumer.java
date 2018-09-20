package net.wbz.selectrix4java.bus.consumption;

/**
 * Consumer for a specific address on a bus.
 *
 * @author Daniel Tuerk
 */
abstract public class BusAddressDataConsumer extends AbstractBusDataConsumer {

    private final int address;

    /**
     * Create consumer for given bus and address.
     *
     * @param bus bus number
     * @param address address of bus
     */
    protected BusAddressDataConsumer(int bus, int address) {
        super(bus);
        this.address = address;
    }

    /**
     * Data value of the address has changed.
     *
     * @param oldValue old value
     * @param newValue new value
     */
    abstract public void valueChanged(int oldValue, int newValue);

    public int getAddress() {
        return address;
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

        BusAddressDataConsumer that = (BusAddressDataConsumer) o;

        return address == that.address;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + address;
        return result;
    }
}
