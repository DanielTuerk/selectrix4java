package net.wbz.selectrix4java;

import net.wbz.selectrix4java.bus.BusAddress;
import net.wbz.selectrix4java.bus.consumption.BusDataConsumer;

import java.util.List;

/**
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public interface Module {

    public int getBus();

    public int getAddress();

    public BusDataConsumer getConsumer();
}
