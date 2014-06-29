package net.wbz.selectrix4java.api.block;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.wbz.selectrix4java.api.Module;
import net.wbz.selectrix4java.api.bus.BusAddress;
import net.wbz.selectrix4java.api.bus.BusAddressListener;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

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
     * Storage of the actual occupied states of this block.
     */
    private Map<Integer, Boolean> blockStates = Maps.newHashMap();

    /**
     * Create a new module with the main address and additional function addresses.
     *
     * @param address             {@link net.wbz.selectrix4java.api.bus.BusAddress}
     * @param additionalAddresses additional addresses (e.g. function decoder)
     */
    public BlockModule(BusAddress address, BusAddress... additionalAddresses) {
        this.address = address;
        this.additionalAddresses = Lists.newArrayList(additionalAddresses);

        address.addListener(new BusAddressListener() {
            @Override
            public void dataChanged(byte oldValue, byte newValue) {
                // block (1-8) occupied state
                for (int i = 1; i < 9; i++) {
                    boolean state = BigInteger.valueOf(newValue).testBit(i - 1);
                    if (blockStates.containsKey(i)) {
                        if (blockStates.get(i) != state) {
                            // state change
                            dispatcher.fireBlockState(i, state);
                        }
                    } else {
                        // first state received
                        dispatcher.fireBlockState(i, state);
                    }
                    blockStates.put(i, state);
                }
            }
        });
    }

    /**
     * Add {@link net.wbz.selectrix4java.api.block.BlockListener} to receive the occupied events.
     *
     * @param listener {@link net.wbz.selectrix4java.api.block.BlockListener}
     */
    public void addBlockListener(BlockListener listener) {
        dispatcher.addListener(listener);
    }

    /**
     * Remove an existing listener.
     *
     * @param listener {@link net.wbz.selectrix4java.api.block.BlockListener}
     */
    public void removeBlockListener(BlockListener listener) {
        dispatcher.removeListener(listener);
    }

    @Override
    public BusAddress getAddress() {
        return address;
    }

    @Override
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
