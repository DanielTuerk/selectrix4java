package io.github.danieltuerk.selectrix4java;

import java.util.List;
import io.github.danieltuerk.selectrix4java.bus.BusAddress;
import io.github.danieltuerk.selectrix4java.bus.consumption.AbstractBusDataConsumer;

/**
 * @author Daniel Tuerk
 */
public interface Module {

    int getBus();

    int getAddress();

    BusAddress getBusAddress();

    List<AbstractBusDataConsumer> getConsumers();
}
