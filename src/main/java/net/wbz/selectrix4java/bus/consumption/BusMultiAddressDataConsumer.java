package net.wbz.selectrix4java.bus.consumption;

import java.util.Arrays;
import java.util.Collection;

/**
 * Consumers are informed by state changes of the configured bus and address.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
abstract public class BusMultiAddressDataConsumer extends AbstractBusDataConsumer {

    private int[] addresses;

    /**
     * Create consumer for the given bus and addresses.
     *
     * @param bus number of bus
     * @param addresses addresses of bus
     */
    protected BusMultiAddressDataConsumer(int bus, int... addresses) {
        super(bus);
        this.addresses = addresses;
    }

    /**
     * Data value of the addresses has changed. Each address with new data value is given by the {@link BusAddressData}.
     *
     * @param data collection of {@link BusAddressData}
     */
    abstract public void valueChanged(Collection<BusAddressData> data);

    public int[] getAddresses() {
        return addresses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        BusMultiAddressDataConsumer that = (BusMultiAddressDataConsumer) o;

        if (!Arrays.equals(addresses, that.addresses))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (addresses != null ? Arrays.hashCode(addresses) : 0);
        return result;
    }
}
