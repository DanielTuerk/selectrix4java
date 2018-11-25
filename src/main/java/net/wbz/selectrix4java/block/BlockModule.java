package net.wbz.selectrix4java.block;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.wbz.selectrix4java.Module;
import net.wbz.selectrix4java.bus.BusAddress;
import net.wbz.selectrix4java.bus.consumption.AbstractBusDataConsumer;
import net.wbz.selectrix4java.bus.consumption.BusAddressDataConsumer;

/**
 * Retrieve the occupied state of the track blocks.
 *
 * @author Daniel Tuerk
 */
public class BlockModule implements Module {

    /**
     * Dispatcher to fire asynchronous the train events to the listeners.
     */
    private final BlockModuleDataDispatcher<BlockListener> dispatcher = new BlockModuleDataDispatcher<>();
    private final BusAddress busAddress;
    private final List<AbstractBusDataConsumer> consumers = new ArrayList<>();
    /**
     * Storage of the actual occupied states of this block. Will be initial filled by the consumer after established
     * connection.
     */
    private final Map<Integer, Boolean> blockStates = new ConcurrentHashMap<>();

    /**
     * Create a new module with the main address and additional function addresses.
     *
     * @param busAddress {@link net.wbz.selectrix4java.bus.BusAddress}
     */
    public BlockModule(BusAddress busAddress) {
        this.busAddress = busAddress;

        consumers.add(new BusAddressDataConsumer(busAddress.getBus(), busAddress.getAddress()) {
            @Override
            public synchronized void valueChanged(int oldValue, int newValue) {
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
     * Reset the module.
     */
    public void reset() {
        // nothing to reset
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
        return busAddress.getBus();
    }

    @Override
    public int getAddress() {
        return busAddress.getAddress();
    }

    @Override
    public BusAddress getBusAddress() {
        return busAddress;
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
        assert blockNr >= 1 && blockNr <= 8;
        return blockStates.get(blockNr);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BlockModule that = (BlockModule) o;
        return Objects.equal(busAddress, that.busAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(busAddress);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("busAddress", busAddress).toString();
    }
}
