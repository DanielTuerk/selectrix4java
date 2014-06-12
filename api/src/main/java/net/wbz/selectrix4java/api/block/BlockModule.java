package net.wbz.selectrix4java.api.block;

import com.google.common.collect.Lists;
import net.wbz.selectrix4java.api.Module;
import net.wbz.selectrix4java.api.bus.BusAddress;
import net.wbz.selectrix4java.api.bus.BusAddressListener;
import net.wbz.selectrix4java.api.train.TrainDataDispatcher;
import net.wbz.selectrix4java.api.train.TrainDataListener;

import java.math.BigInteger;
import java.util.List;

/**
 * Retrieve the occupied state of the track blocks.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class BlockModule implements Module {

    /**
     * Main address of the block.
     */
    private final BusAddress address;

    /**
     * Additional function addresses of the block.
     */
    private final List<BusAddress> additionalAddresses;

    /**
     * Dispatcher to fire asynchronous the train events to the listeners.
     */
    private final BlockModuleDataDispatcher<BlockListener> dispatcher = new BlockModuleDataDispatcher<>();

    /**
     * Create a new module with the main address and additional function addresses.
     *
     * @param address             {@link net.wbz.selectrix4java.api.bus.BusAddress}
     * @param additionalAddresses additional addresses (e.g. function decoder)
     */
    public BlockModule(BusAddress address, BusAddress... additionalAddresses) {
        this.address = address;
        this.additionalAddresses=Lists.newArrayList(additionalAddresses);
        address.addListener(new BusAddressListener() {
            @Override
            public void dataChanged(byte oldValue, byte newValue) {
                // block occupied state
                // TODO fire
            }
        });
    }

    public void addBlockListener(BlockListener listener) {
        dispatcher.addListener(listener);
    }

    public void removeBlockListener(BlockListener listener) {
        dispatcher.removeListener(listener);
    }


    public BusAddress getAddress() {
        return address;
    }

    public List<BusAddress> getAdditionalAddresses() {
        return additionalAddresses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BlockModule that = (BlockModule) o;

        return address.equals(that.address);

    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }
}
