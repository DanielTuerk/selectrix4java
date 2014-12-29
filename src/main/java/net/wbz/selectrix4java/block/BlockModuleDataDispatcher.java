package net.wbz.selectrix4java.block;

import net.wbz.selectrix4java.AbstractModuleDataDispatcher;

/**
 * Dispatcher for async call of the registered {@link net.wbz.selectrix4java.block.BlockListener}s.
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
            fireBlockOccupied(blockNr);
        } else {
            fireBlockFreed(blockNr);
        }
    }

}