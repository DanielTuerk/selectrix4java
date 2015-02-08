package net.wbz.selectrix4java.bus.consumption;

/**
 * Consumers are informed by state changes of the configured bus and address.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
abstract public class BusDataConsumer {

    private int bus;
    private boolean called = false;

    protected BusDataConsumer(int bus) {
        this.bus = bus;
    }

    public boolean isCalled() {
        return called;
    }

    public void setCalled(boolean called) {
        this.called = called;
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

        BusDataConsumer that = (BusDataConsumer) o;

        if (bus != that.bus) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = bus;
        result = 31 * result;
        return result;
    }

}
