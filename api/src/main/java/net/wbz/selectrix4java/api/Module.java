package net.wbz.selectrix4java.api;

import net.wbz.selectrix4java.api.bus.BusAddress;

import java.util.List;

/**
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public interface Module {

    public BusAddress getAddress();

    public List<BusAddress> getAdditionalAddresses();
}
