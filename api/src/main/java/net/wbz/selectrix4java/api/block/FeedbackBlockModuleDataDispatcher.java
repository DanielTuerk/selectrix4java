package net.wbz.selectrix4java.api.block;

/**
 * Dispatcher for async call of the registered {@link net.wbz.selectrix4java.api.block.FeedbackBlockListener}s.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class FeedbackBlockModuleDataDispatcher extends BlockModuleDataDispatcher<FeedbackBlockListener> {

    public void fireTrainEnterBlock(final int blockNumber, final int speed, final byte trainAddress){
        fire(new ListenerRunnable() {
            @Override
            public void run() {
                getListener().trainEnterBlock(blockNumber, speed, trainAddress);
            }
        });
    }
    public void fireTrainLeaveBlock(final int blockNumber, final int speed, final byte trainAddress){
        fire(new ListenerRunnable() {
            @Override
            public void run() {
                getListener().trainLeaveBlock(blockNumber, speed, trainAddress);
            }
        });
    }

}
