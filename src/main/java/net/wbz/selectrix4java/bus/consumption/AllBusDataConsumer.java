package net.wbz.selectrix4java.bus.consumption;

/**
 * This consumer is informed by state changes of all addresses of each existing SX bus.
 *
 * @author Daniel Tuerk
 */
abstract public class AllBusDataConsumer extends AbstractBusDataConsumer {

    protected AllBusDataConsumer() {
        super(-1);
    }

    abstract public void valueChanged(int bus, int address, int oldValue, int newValue);
}
