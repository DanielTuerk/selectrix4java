package net.wbz.selectrix4java.block;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import net.wbz.selectrix4java.bus.BusAddress;
import net.wbz.selectrix4java.bus.consumption.BusAddressData;
import net.wbz.selectrix4java.bus.consumption.BusMultiAddressDataConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Block which use the feedback address to receive the train address on the block.
 * <p>
 * Control the block to update the speed of each entering or exiting train on the block.</p>
 * <p>Supported Devices:</p>
 * <ul>
 * <li>D&amp;H Belegtmelder 8i</li>
 * </ul>
 *
 * TODO should be the D&amp;H 8i - implementation
 *
 * @author Daniel Tuerk
 */
public class FeedbackBlockModule extends BlockModule {

    private static final Logger log = LoggerFactory.getLogger(FeedbackBlockModule.class);

    private final FeedbackBlockModuleDataDispatcher dispatcher = new FeedbackBlockModuleDataDispatcher();

    /**
     * Mapping of trains which where detected in this feedback block. Each train map to the last received train data.
     * Used to avoid duplicated events.
     */
    private final Map<Integer, FeedbackTrainData> trainAddressLastSend = Maps.newConcurrentMap();
    private final BusAddress feedbackAddress;
    private final BusAddress additionalAddress;

    public FeedbackBlockModule(BusAddress busAddress, final BusAddress feedbackAddress, BusAddress additionalAddress) {
        super(busAddress);
        this.feedbackAddress = feedbackAddress;
        this.additionalAddress = additionalAddress;

        getConsumers().add(new BusMultiAddressDataConsumer(busAddress.getBus(), additionalAddress.getAddress(),
                feedbackAddress.getAddress()) {

            @Override
            public synchronized void valueChanged(Collection<BusAddressData> data) {
                log.trace("valueChanged data: {}", data);

                if (data.size() != 2) {
                    log.error("data size not 2: {}", data);
                    return;
                }

                handleReceivedFeedbackData(data);
            }
        });

    }

    private void handleReceivedFeedbackData(Collection<BusAddressData> data) {
        int stateAddressNewDataValue = -1;
        int feedbackAddressNewDataValue = -1;

        for (BusAddressData addressData : data) {
            if (addressData.getAddress() == FeedbackBlockModule.this.additionalAddress.getAddress()) {
                stateAddressNewDataValue = addressData.getNewDataValue();
            } else if (addressData.getAddress() == FeedbackBlockModule.this.feedbackAddress.getAddress()) {
                if (addressData.getNewDataValue() > 0 && addressData.getNewDataValue() < 127) {
                    // initial 0 is called
                    feedbackAddressNewDataValue = addressData.getNewDataValue();
                }
            }
        }

        if (stateAddressNewDataValue != -1 && feedbackAddressNewDataValue != -1) {
            int sequenceNr = stateAddressNewDataValue & 0x60;
            log.trace("sequence {}", sequenceNr);

            FeedbackTrainData feedbackTrainData = new FeedbackTrainData();

            BigInteger wrappedNewDataValue = BigInteger.valueOf(stateAddressNewDataValue);

            feedbackTrainData.setBlockNr((stateAddressNewDataValue & 0x7) + 1);
            feedbackTrainData.setEnteringBlock(wrappedNewDataValue.testBit(3));
            feedbackTrainData.setTrainDirectionForward(wrappedNewDataValue.testBit(4));
            feedbackTrainData.setTrainAddress(feedbackAddressNewDataValue);

            boolean isDuplicate = false;
            if (trainAddressLastSend.containsKey(feedbackTrainData.getTrainAddress())) {
                isDuplicate = trainAddressLastSend.get(feedbackTrainData.getTrainAddress()).equals(feedbackTrainData);
            }
            if (!isDuplicate) {
                trainAddressLastSend.put(feedbackTrainData.getTrainAddress(), feedbackTrainData);
                if (feedbackTrainData.isEnteringBlock()) {
                    dispatcher.fireTrainEnterBlock(feedbackTrainData.getBlockNr(), feedbackTrainData.getTrainAddress(),
                            feedbackTrainData.isTrainDirectionForward());
                } else {
                    dispatcher.fireTrainLeaveBlock(feedbackTrainData.getBlockNr(), feedbackTrainData.getTrainAddress(),
                            feedbackTrainData.isTrainDirectionForward());
                }
            } else {
                log.error("duplicate: {}", feedbackTrainData);
            }

        } else {
            log.error("state ({}) and feedback ({}) not new", stateAddressNewDataValue, feedbackAddressNewDataValue);
        }
    }

    /**
     * Rest states and request new states from module. Sends the request command. New states are received by the
     * consumer and delegated to listeners.
     *
     * @see #handleReceivedFeedbackData(Collection)
     */
    public void requestNewFeedbackData() {
        reset();
        // only bit 8
        additionalAddress.sendData((byte) 128);
    }

    /**
     * Reset all feedback states which are cached as send state. This trigger the send of all next states which are
     * received by the address consumers.
     */
    @Override
    public void reset() {
        super.reset();
        trainAddressLastSend.clear();
    }

    /**
     * Add the given {@link net.wbz.selectrix4java.block.FeedbackBlockListener} to receive state changes of the block.
     *
     * @param listener {@link net.wbz.selectrix4java.block.FeedbackBlockListener}
     */
    public void addFeedbackBlockListener(FeedbackBlockListener listener) {
        addBlockListener(listener);
        dispatcher.addListener(listener);
    }

    /**
     * Remove existing listener.
     *
     * @param listener {@link net.wbz.selectrix4java.block.FeedbackBlockListener}
     */
    public void removeFeedbackBlockListener(FeedbackBlockListener listener) {
        removeBlockListener(listener);
        dispatcher.removeListener(listener);
    }

    @Override
    public String toString() {
        return super.toString() + Objects.toStringHelper(this).add("dispatcher", dispatcher).toString();
    }

    /**
     * Model to store information about train data on block.
     */
    private class FeedbackTrainData {

        private int trainAddress;
        private boolean trainDirectionForward;
        private int blockNr;
        private boolean enteringBlock;

        FeedbackTrainData() {
        }

        public int getTrainAddress() {
            return trainAddress;
        }

        public void setTrainAddress(int trainAddress) {
            this.trainAddress = trainAddress;
        }

        boolean isTrainDirectionForward() {
            return trainDirectionForward;
        }

        void setTrainDirectionForward(boolean trainDirectionForward) {
            this.trainDirectionForward = trainDirectionForward;
        }

        public int getBlockNr() {
            return blockNr;
        }

        public void setBlockNr(int blockNr) {
            this.blockNr = blockNr;
        }

        boolean isEnteringBlock() {
            return enteringBlock;
        }

        void setEnteringBlock(boolean enteringBlock) {
            this.enteringBlock = enteringBlock;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("train", trainAddress).add("forward", trainDirectionForward)
                    .add("blockNr", blockNr).add("enteringBlock", enteringBlock).toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FeedbackTrainData that = (FeedbackTrainData) o;
            return Objects.equal(trainAddress, that.trainAddress) && Objects
                    .equal(trainDirectionForward, that.trainDirectionForward) && Objects.equal(blockNr, that.blockNr)
                    && Objects.equal(enteringBlock, that.enteringBlock);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(trainAddress, trainDirectionForward, blockNr, enteringBlock);
        }
    }

}
