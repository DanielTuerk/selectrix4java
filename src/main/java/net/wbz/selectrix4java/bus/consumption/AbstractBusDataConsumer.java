package net.wbz.selectrix4java.bus.consumption;

/**
 * Consumers are informed by state changes of the configured bus and address.
 *
 * @author Daniel Tuerk
 */
abstract public class AbstractBusDataConsumer {

    private final int bus;

    protected AbstractBusDataConsumer(int bus) {
        this.bus = bus;
    }

    public int getBus() {
        return bus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractBusDataConsumer that = (AbstractBusDataConsumer) o;

        return bus == that.bus;

    }

    @Override
    public int hashCode() {
        int result = bus;
        result = 31 * result;
        return result;
    }

}
