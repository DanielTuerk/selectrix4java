package net.wbz.selectrix4java.block;

/**
 * Listener for trains on an {@link net.wbz.selectrix4java.block.FeedbackBlockModule}.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public interface FeedbackBlockListener extends BlockListener {

    public void trainEnterBlock(int blockNumber, int trainAddress, boolean drivingDirection);

    public void trainLeaveBlock(int blockNumber, int trainAddress, boolean drivingDirection);

}
