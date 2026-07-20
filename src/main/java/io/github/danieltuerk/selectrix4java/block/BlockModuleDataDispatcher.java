package io.github.danieltuerk.selectrix4java.block;

import io.github.danieltuerk.selectrix4java.AbstractModuleDataDispatcher;

/**
 * Dispatcher for async call of the registered {@link io.github.danieltuerk.selectrix4java.block.BlockListener}s.
 *
 * @author Daniel Tuerk
 */
public class BlockModuleDataDispatcher<T extends BlockListener> extends AbstractModuleDataDispatcher<T> {

    public void fireBlockOccupied(final int blockNr) {
        for (T listener : getListeners()) {
            listener.blockOccupied(blockNr);
        }

    }

    public void fireBlockFreed(final int blockNr) {
        for (T listener : getListeners()) {
            listener.blockFreed(blockNr);
        }

    }

    public void fireBlockState(int blockNr, boolean state) {
        if (state) {
            fireBlockOccupied(blockNr);
        } else {
            fireBlockFreed(blockNr);
        }
    }

}
