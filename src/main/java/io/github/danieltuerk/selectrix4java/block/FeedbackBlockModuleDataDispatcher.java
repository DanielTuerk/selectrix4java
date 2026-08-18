package io.github.danieltuerk.selectrix4java.block;

/**
 * Dispatcher for async call of the registered {@link io.github.danieltuerk.selectrix4java.block.FeedbackBlockListener}s.
 *
 * @author Daniel Tuerk
 */
public class FeedbackBlockModuleDataDispatcher extends BlockModuleDataDispatcher<FeedbackBlockListener> {

    /**
     * Create new dispatcher without registered listeners.
     */
    public FeedbackBlockModuleDataDispatcher() {
    }

    /**
     * Inform all listeners that a train entered the block.
     *
     * @param blockNumber  number of the block
     * @param trainAddress address of the train
     * @param forward      driving direction of the train
     */
    public void fireTrainEnterBlock(final int blockNumber, final int trainAddress, final boolean forward) {
        for (FeedbackBlockListener listener : getListeners()) {
            listener.trainEnterBlock(blockNumber, trainAddress, forward);
        }
    }

    /**
     * Inform all listeners that a train left the block.
     *
     * @param blockNumber number of the block
     * @param trainAddress address of the train
     * @param forward driving direction of the train
     */
    public void fireTrainLeaveBlock(final int blockNumber, final int trainAddress, final boolean forward) {
        for (FeedbackBlockListener listener : getListeners()) {
            listener.trainLeaveBlock(blockNumber, trainAddress, forward);
        }
    }

}
