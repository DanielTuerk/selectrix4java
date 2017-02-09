package net.wbz.selectrix4java.block;

/**
 * Dispatcher for async call of the registered {@link net.wbz.selectrix4java.block.FeedbackBlockListener}s.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class FeedbackBlockModuleDataDispatcher extends BlockModuleDataDispatcher<FeedbackBlockListener> {

    public void fireTrainEnterBlock(final int blockNumber, final int trainAddress, final boolean forward) {
        for (FeedbackBlockListener listener : getListeners()) {
            listener.trainEnterBlock(blockNumber, trainAddress, forward);
        }
        // fire(new ListenerRunnable() {
        // @Override
        // public void run() {
        // getListener().trainEnterBlock(blockNumber, trainAddress, forward);
        // }
        // });
    }

    public void fireTrainLeaveBlock(final int blockNumber, final int trainAddress, final boolean forward) {
        for (FeedbackBlockListener listener : getListeners()) {
            listener.trainLeaveBlock(blockNumber, trainAddress, forward);
        }
        // fire(new ListenerRunnable() {
        // @Override
        // public void run() {
        // getListener().trainLeaveBlock(blockNumber, trainAddress, forward);
        // }
        // });
    }

}
