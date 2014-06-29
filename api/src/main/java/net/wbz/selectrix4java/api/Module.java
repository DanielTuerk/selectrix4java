package net.wbz.selectrix4java.api;

import net.wbz.selectrix4java.api.bus.BusAddress;

import java.util.List;

/**
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public interface Module {

    /**
     * Address of the module.
     *
     * @return {@link net.wbz.selectrix4java.api.bus.BusAddress}
     */
    public BusAddress getAddress();

    /**
     * Optional addresses of the module.
     *
     * @return {@link java.util.List<net.wbz.selectrix4java.api.bus.BusAddress>}
     */
    public List<BusAddress> getAdditionalAddresses();
}
