package net.wbz.selectrix4java;

import java.util.List;
import net.wbz.selectrix4java.bus.BusAddress;
import net.wbz.selectrix4java.bus.consumption.AbstractBusDataConsumer;

/**
 * @author Daniel Tuerk
 */
public interface Module {

    int getBus();

    int getAddress();

    BusAddress getBusAddress();

    List<AbstractBusDataConsumer> getConsumers();
}
