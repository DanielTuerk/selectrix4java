package net.wbz.selectrix4java;

import net.wbz.selectrix4java.bus.BusAddress;

import java.util.List;

/**
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public interface Module {

    /**
     * Address of the module.
     *
     * @return {@link net.wbz.selectrix4java.bus.BusAddress}
     */
    public BusAddress getAddress();

    /**
     * Optional addresses of the module.
     *
     * @return {@link java.util.List<net.wbz.selectrix4java.bus.BusAddress>}
     */
    public List<BusAddress> getAdditionalAddresses();
}
