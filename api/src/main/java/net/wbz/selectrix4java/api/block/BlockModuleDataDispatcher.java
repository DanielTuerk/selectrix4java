package net.wbz.selectrix4java.api.block;

import net.wbz.selectrix4java.api.AbstractModuleDataDispatcher;
import net.wbz.selectrix4java.api.train.TrainDataListener;
import net.wbz.selectrix4java.api.train.TrainModule;

/**
 * Dispatcher for async call of the registered {@link net.wbz.selectrix4java.api.block.BlockListener}s.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class BlockModuleDataDispatcher<T extends BlockListener> extends AbstractModuleDataDispatcher<T> {

    public void fireBlockOccupied(final int blockNr){
        fire(new ListenerRunnable() {
            @Override
            public void run() {
                getListener().blockOccupied(blockNr);
            }
        });
    }
    public void fireBlockFreed(final int blockNr){
        fire(new ListenerRunnable() {
            @Override
            public void run() {
                getListener().blockFreed(blockNr);
            }
        });
    }

    public void fireBlockState(int blockNr, boolean state) {
        if(state) {
            fireBlockFreed(blockNr);
        } else {
            fireBlockOccupied(blockNr);
        }
    }

}
