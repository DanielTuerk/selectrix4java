package net.wbz.selectrix4java.block;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import net.wbz.selectrix4java.bus.BusAddress;
import net.wbz.selectrix4java.bus.consumption.BusAddressData;
import net.wbz.selectrix4java.bus.consumption.BusMultiAddressDataConsumer;

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
 * TODO should be the D&H 8i - implementation
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class FeedbackBlockModule extends BlockModule {

    private static final Logger log = LoggerFactory.getLogger(FeedbackBlockModule.class);

    /**
     * TODO doc
     */
    private final ExecutorService executorService;

    private final FeedbackBlockModuleDataDispatcher dispatcher = new FeedbackBlockModuleDataDispatcher();

    /**
     * TODO avoid duplicated events? but how with exeutor?
     * Mapping of trains which where detected in this feedback block. Each train map to the last received train data.
     */
    private final Map<Integer, FeedbackTrainData> trainAddressEventMapping = Maps.newConcurrentMap();
    private final BusAddress feedbackAddress;
    private final BusAddress additionalAddress;

    public FeedbackBlockModule(BusAddress busAddress, BusAddress feedbackAddress, BusAddress additionalAddress) {
        super(busAddress);
        this.feedbackAddress = feedbackAddress;
        this.additionalAddress = additionalAddress;

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("feedback-block-%d").build();
        executorService = Executors.newCachedThreadPool(namedThreadFactory);

        getConsumers().add(new BusMultiAddressDataConsumer(busAddress.getBus(), additionalAddress.getAddress(),
                feedbackAddress.getAddress()) {
            /**
             * Last sequence number which was used to throw an event.
             * Only the first unique number could be used to indicate the train state at the block.
             */
            private int lastSequenceNr = -1;

            /**
             * Last train address which was detected. Is only fired once it changes. State changes on blocks belong to
             * the same train, until a new train address is thrown.
             */
            private int lastTrainAddress = -1;

            private FeedbackTrainData lastData = new FeedbackTrainData();

            private Future<?> future;

            @Override
            public synchronized void valueChanged(Collection<BusAddressData> data) {
                log.trace("valueChanged last data: {}; last sequence {} -- {}", new Object[] { lastData,
                        lastSequenceNr, data });

                // check for new train address
                Integer train = null;
                for (BusAddressData addressData : data) {
                    if (addressData.getAddress() == FeedbackBlockModule.this.feedbackAddress.getAddress()) {
                        int newDataValue = addressData.getNewDataValue();
                        log.debug("new train {}", newDataValue);
                        train = newDataValue;
                        lastTrainAddress = newDataValue;
                    }
                }

                for (BusAddressData addressData : data) {
                    // check train state on block number
                    if (addressData.getAddress() == FeedbackBlockModule.this.additionalAddress.getAddress()) { // &&
                                                                                                               // lastTrainAddress
                                                                                                               // > 0

                        /*
                         * Only first unique sequence number throw an event, next data of same sequence number could be
                         * corrupt.
                         */
                        int sequenceNr = addressData.getNewDataValue() & 0x60;
                        log.trace("new sequence {}", sequenceNr);

                        if (sequenceNr != lastSequenceNr) {
                            // new data - immediately start pending data
                            lastData.setReady(true);
                            // wait for finish
                            if (future != null) {
                                try {
                                    future.get();
                                } catch (InterruptedException | ExecutionException e) {
                                    log.error("join future", e);
                                }
                            }
                            lastSequenceNr = sequenceNr;
                            lastData = new FeedbackTrainData();

                        } else {
                            log.warn("getting the same sequence number last data: {}; last train: {}", lastData,
                                    lastTrainAddress);
                            return;
                        }

                        int blockNr = (addressData.getNewDataValue() & 0x7) + 1;

                        BigInteger wrappedNewDataValue = BigInteger.valueOf(addressData.getNewDataValue());
                        boolean enter = wrappedNewDataValue.testBit(3);
                        boolean forward = wrappedNewDataValue.testBit(4);

                        if (train == null) {
                            lastData.setTrainAddress(lastTrainAddress);
                        } else {
                            lastData.setTrainAddress(train);
                        }
                        lastData.setBlockNr(blockNr);
                        lastData.setEnteringBlock(enter);
                        lastData.setTrainDirectionForward(forward);

                    }
                }

                if (lastData.isComplete() && (future == null || future.isDone())) {
                    log.trace("feedback module ({}) - submit last Data: {}", new Object[] { getBusAddress()
                            .getAddress(),
                            lastData });
                    future = submitDispatcherCall(lastData, getBusAddress().getAddress());
                } else {
                    // correct the data
                    lastData.setTrainAddress(train);
                    log.trace("only update train data");
                }

            }
        });

    }

    private Future<?> submitDispatcherCall(final FeedbackTrainData lastData, final int address) {
        return executorService.submit(new Runnable() {
            @Override
            public void run() {

                int secondsCount = 0;
                while (secondsCount < 5) {
                    if (lastData.isReady()) {
                        // stop waiting
                        break;
                    }
                    try {
                        // TODO less waiting
                        Thread.sleep(50L);
                    } catch (InterruptedException e) {
                        log.error("error waiting for ready of feedback data {}", lastData, e);
                    }
                    secondsCount++;
                }

                log.trace("Block {}: send -> {}", address, lastData);
                if (lastData.isComplete()) {
                    if (lastData.getEnteringBlock() != null) {
                        if (lastData.getEnteringBlock()) {
                            dispatcher.fireTrainEnterBlock(lastData.getBlockNr(), lastData.getTrainAddress(), lastData
                                    .getTrainDirectionForward());
                        } else {
                            dispatcher.fireTrainLeaveBlock(lastData.getBlockNr(), lastData.getTrainAddress(), lastData
                                    .getTrainDirectionForward());
                        }
                    }
                }
            }
        });
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

    /**
     * Send the state request for the feedback module.
     * The module get response for all actual train block information of the additional and feedback address.
     */
    public void requestCurrentFeedbackState() {
        additionalAddress.setBit(8).send();
    }

    @Override
    public String toString() {
        return super.toString() + Objects.toStringHelper(this)
                .add("dispatcher", dispatcher)
                .toString();
    }

    /**
     * Model to store information about train data on block.
     */
    private class FeedbackTrainData {
        private Integer trainAddress;
        private Boolean trainDirectionForward;
        private Integer blockNr;
        private Boolean enteringBlock;

        /**
         * State to immediately call the dispatcher.
         */
        private boolean ready = false;

        FeedbackTrainData() {
        }

        boolean isReady() {
            return ready;
        }

        void setReady(boolean ready) {
            this.ready = ready;
        }

        boolean isComplete() {
            return trainAddress != null
                    && trainAddress > 0
                    && trainDirectionForward != null
                    && blockNr != null
                    && enteringBlock != null;
        }

        Integer getTrainAddress() {
            return trainAddress;
        }

        void setTrainAddress(Integer trainAddress) {
            this.trainAddress = trainAddress;
        }

        Boolean getTrainDirectionForward() {
            return trainDirectionForward;
        }

        void setTrainDirectionForward(Boolean trainDirectionForward) {
            this.trainDirectionForward = trainDirectionForward;
        }

        Integer getBlockNr() {
            return blockNr;
        }

        void setBlockNr(Integer blockNr) {
            this.blockNr = blockNr;
        }

        Boolean getEnteringBlock() {
            return enteringBlock;
        }

        void setEnteringBlock(Boolean enteringBlock) {
            this.enteringBlock = enteringBlock;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("train", trainAddress)
                    .add("forward", trainDirectionForward)
                    .add("blockNr", blockNr)
                    .add("enteringBlock", enteringBlock)
                    .toString();
        }
    }

}
