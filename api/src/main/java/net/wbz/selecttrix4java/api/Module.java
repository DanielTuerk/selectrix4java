package net.wbz.selecttrix4java.api;

import net.wbz.selecttrix4java.api.bus.BusAddress;
import net.wbz.selecttrix4java.api.bus.BusDataConsumer;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public interface Module {

    public BusAddress getAddress();

    public List<BusAddress> getAdditionalAddresses();
}
