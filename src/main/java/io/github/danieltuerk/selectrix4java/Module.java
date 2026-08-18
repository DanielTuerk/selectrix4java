package io.github.danieltuerk.selectrix4java;

import io.github.danieltuerk.selectrix4java.bus.BusAddress;
import io.github.danieltuerk.selectrix4java.bus.consumption.AbstractBusDataConsumer;

import java.util.List;

/**
 * Functional representation of a bus address (e.g. a train or block module).
 *
 * @author Daniel Tuerk
 */
public interface Module {

    /**
     * Bus number of the module.
     *
     * @return bus number
     */
    int getBus();

    /**
     * Address of the module on the bus.
     *
     * @return address
     */
    int getAddress();

    /**
     * {@link BusAddress} of the module.
     *
     * @return bus address
     */
    BusAddress getBusAddress();

    /**
     * Consumers registered for this module.
     *
     * @return consumers
     */
    List<AbstractBusDataConsumer> getConsumers();
}
