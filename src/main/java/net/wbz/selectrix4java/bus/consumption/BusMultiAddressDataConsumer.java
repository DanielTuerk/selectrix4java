package net.wbz.selectrix4java.bus.consumption;

import java.util.Arrays;
import java.util.Collection;

/**
 * Consumers are informed by state changes of the configured bus and addresses. Is called if at least one address data
 * has changed. All other data of configured addresses is send with the old value.
 *
 * @author Daniel Tuerk
 */
abstract public class BusMultiAddressDataConsumer extends AbstractBusDataConsumer {

    private final int[] addresses;

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
     * Data value of the addresses. Each address data value is given by the {@link BusAddressData}. At least one address
     * with new data value was detected.
     *
     * @param data collection of {@link BusAddressData}
     */
    abstract public void valueChanged(Collection<BusAddressData> data);

    public int[] getAddresses() {
        return addresses;
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

        BusMultiAddressDataConsumer that = (BusMultiAddressDataConsumer) o;

        return Arrays.equals(addresses, that.addresses);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (addresses != null ? Arrays.hashCode(addresses) : 0);
        return result;
    }
}
