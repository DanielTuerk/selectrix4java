package io.github.danieltuerk.selectrix4java.block;

/**
 * Listener for trains on an {@link io.github.danieltuerk.selectrix4java.block.FeedbackBlockModule}.
 *
 * @author Daniel Tuerk
 */
public interface FeedbackBlockListener extends BlockListener {

    /**
     * A train has entered the block.
     *
     * @param blockNumber  number of the block
     * @param trainAddress address of the train
     * @param forward      driving direction of the train
     */
    void trainEnterBlock(int blockNumber, int trainAddress, boolean forward);

    /**
     * A train has left the block.
     *
     * @param blockNumber number of the block
     * @param trainAddress address of the train
     * @param forward driving direction of the train
     */
    void trainLeaveBlock(int blockNumber, int trainAddress, boolean forward);

}
