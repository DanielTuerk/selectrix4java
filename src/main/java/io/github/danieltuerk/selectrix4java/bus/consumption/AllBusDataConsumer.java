package io.github.danieltuerk.selectrix4java.bus.consumption;

/**
 * This consumer is informed by state changes of all addresses of each existing SX bus.
 *
 * @author Daniel Tuerk
 */
abstract public class AllBusDataConsumer extends AbstractBusDataConsumer {

    /**
     * Create consumer for all buses and addresses.
     */
    protected AllBusDataConsumer() {
        super(-1);
    }

    /**
     * Data value of the address has changed.
     *
     * @param bus      bus number
     * @param address  address on the bus
     * @param oldValue old value
     * @param newValue new value
     */
    abstract public void valueChanged(int bus, int address, int oldValue, int newValue);
}
