package net.wbz.selectrix4java.block;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import net.wbz.selectrix4java.Module;
import net.wbz.selectrix4java.bus.consumption.AbstractBusDataConsumer;
import net.wbz.selectrix4java.bus.consumption.BusAddressDataConsumer;

/**
 * Retrieve the occupied state of the track blocks.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class BlockModule implements Module {

    private final int bus;
    private final int address;

    /**
     * Dispatcher to fire asynchronous the train events to the listeners.
     */
    private final BlockModuleDataDispatcher<BlockListener> dispatcher = new BlockModuleDataDispatcher<>();

    /**
     * Storage of the actual occupied states of this block.
     */
    private Map<Integer, Boolean> blockStates = Maps.newHashMap();

    private final List<AbstractBusDataConsumer> consumers = new ArrayList<>();
    /**
     * Create a new module with the main address and additional function addresses.
     *
     * @param address             {@link net.wbz.selectrix4java.bus.BusAddress}
     */
    public BlockModule(int bus, int address) {
        this.bus=bus;
        this.address=address;

        consumers.add(new BusAddressDataConsumer(bus, address) {
            @Override
            public void valueChanged(int oldValue, int newValue) {
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
     * Add {@link net.wbz.selectrix4java.block.BlockListener} to receive the occupied events.
     *
     * @param listener {@link net.wbz.selectrix4java.block.BlockListener}
     */
    public void addBlockListener(BlockListener listener) {
        dispatcher.addListener(listener);
    }

    /**
     * Remove an existing listener.
     *
     * @param listener {@link net.wbz.selectrix4java.block.BlockListener}
     */
    public void removeBlockListener(BlockListener listener) {
        dispatcher.removeListener(listener);
    }

    @Override
    public int getBus() {
        return bus;
    }

    @Override
    public int getAddress() {
        return address;
    }

    @Override
    public List<AbstractBusDataConsumer> getConsumers() {
        return consumers;
    }

    /**
     * Last received state for the given block number.
     *
     * @param blockNr number of the block (1-8)
     * @return {@code true} if the block is occupied otherwise it's free
     */
    public boolean getLastReceivedBlockState(int blockNr) {
        assert blockNr >=1 && blockNr <=8;
        return blockStates.get(blockNr);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BlockModule that = (BlockModule) o;

        if (address != that.address) return false;
        if (bus != that.bus) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = bus;
        result = 31 * result + (int) address;
        return result;
    }
}
