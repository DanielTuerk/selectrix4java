package net.wbz.selectrix4java.bus.consumption;

/**
 * @author Daniel Tuerk
 */
abstract public class BusAddressDataConsumer extends AbstractSingleAddressBusDataConsumer {

    private final int address;

    protected BusAddressDataConsumer(int bus, int address) {
        super(bus);
        this.address = address;
    }

    abstract public void valueChanged(int oldValue, int newValue);

    public int getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BusAddressDataConsumer that = (BusAddressDataConsumer) o;

        if (address != that.address) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + address;
        return result;
    }
}
