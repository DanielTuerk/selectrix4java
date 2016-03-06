package net.wbz.selectrix4java.block;

import com.google.common.collect.Maps;
import net.wbz.selectrix4java.Module;
import net.wbz.selectrix4java.bus.consumption.BusAddressData;
import net.wbz.selectrix4java.bus.consumption.AbstractBusDataConsumer;
import net.wbz.selectrix4java.bus.consumption.BusMultiAddressDataConsumer;

import java.math.BigInteger;
import java.util.Map;

/**
 * Block which use the feedback address to receive the train address on the block.
 * <p/>
 * Control the block to update the speed of each entering or exiting train on the block.
 * <p/>
 * <h2>Supported Devices:</h2>
 * <ul>
 * <li>D&H Belegtmelder 8i</li>
 * </ul>
 * <p/>
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class FeedbackBlockModule implements Module {

    private final FeedbackBlockModuleDataDispatcher dispatcher = new FeedbackBlockModuleDataDispatcher();

    private final AbstractBusDataConsumer consumer;
    private final int bus;
    private final int address;

    private final Map<Integer, Block> blockStates = Maps.newConcurrentMap();

    public FeedbackBlockModule(int bus, final int address,
                               final int feedbackAddress, final int additionalAddress) {

        this.bus = bus;
        this.address = address;

        // init blocks
        for (int i = 1; i <= 8; i++) {
            blockStates.put(i, new Block(i));
        }

        consumer = new BusMultiAddressDataConsumer(bus, new int[]{address, feedbackAddress, additionalAddress}) {

            /**
             * Flag to indicate the first call for throwing the events for the initial state of all blocks.
             */
            private boolean initial = true;

            /**
             * Last sequence number which was used to throw an event.
             * Only the first unique number could be used to indicate the train state at the block.
             */
            private int lastSequenceNr = -1;

            @Override
            public void valueChanged(BusAddressData[] data) {

                int train = -1;

                // check train address
                for (BusAddressData addressData : data) {
                    if (addressData.getAddress() == feedbackAddress) {
                        train = addressData.getNewDataValue();
                    }
                }

                // check block state
                for (BusAddressData addressData : data) {
                    if (addressData.getAddress() == address) {
                        for (int blockNr = 1; blockNr < 9; blockNr++) {
                            boolean newState = BigInteger.valueOf(addressData.getNewDataValue()).testBit(blockNr - 1);
                            boolean oldState = BigInteger.valueOf(addressData.getOldDataValue()).testBit(blockNr - 1);
                            if (initial || newState != oldState) {
                                dispatcher.fireBlockState(blockNr, newState);
                            }
                        }
                    }

                    // check train state on block number
                    if (addressData.getAddress() == additionalAddress && train > 0) {

                        // only first unique sequence number throw an event, next data of same sequence number could be corrupt
                        int sequenceNr = addressData.getNewDataValue() & 0x60;
                        if (sequenceNr != lastSequenceNr) {

                            int blockNr = (addressData.getNewDataValue() & 0x7) + 1;

                            BigInteger wrappedAddress = BigInteger.valueOf(addressData.getNewDataValue());
                            boolean enter = wrappedAddress.testBit(3);
                            boolean forward = wrappedAddress.testBit(4);

                            if (enter) {
                                blockStates.get(blockNr).trainEnter(train, forward);
                            } else {
                                blockStates.get(blockNr).trainExit(train, forward);
                            }

                            lastSequenceNr = sequenceNr;
                        }
                    }
                }

                initial = false;
            }
        };
    }

    /**
     * Add the given {@link net.wbz.selectrix4java.block.FeedbackBlockListener} to receive state changes of the block.
     *
     * @param listener {@link net.wbz.selectrix4java.block.FeedbackBlockListener}
     */
    public void addFeedbackBlockListener(FeedbackBlockListener listener) {
        dispatcher.addListener(listener);
    }

    /**
     * Remove existing listener.
     *
     * @param listener {@link net.wbz.selectrix4java.block.FeedbackBlockListener}
     */
    public void removeFeedbackBlockListener(FeedbackBlockListener listener) {
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
    public AbstractBusDataConsumer getConsumer() {
        return consumer;
    }

    /**
     * Model of one singe block to store the trains enter and exit the the block.
     * For each state change an event will be thrown by the
     * {@link net.wbz.selectrix4java.block.FeedbackBlockModuleDataDispatcher}.
     */
    private class Block {

        private int blockNr;
        private final Map<Integer, Boolean> trainBlockStateMap = Maps.newConcurrentMap();

        public Block(int blockNr) {
            this.blockNr = blockNr;
        }

        public void trainEnter(int train, boolean direction) {
            if (!trainBlockStateMap.containsKey(train)) {
                trainBlockStateMap.put(train, direction);
                dispatcher.fireTrainEnterBlock(blockNr, train, direction);
            }
        }

        public void trainExit(int train, boolean direction) {
            if (trainBlockStateMap.containsKey(train)) {
                trainBlockStateMap.remove(train);
                dispatcher.fireTrainLeaveBlock(blockNr, train, direction);
            }
        }
    }
}
