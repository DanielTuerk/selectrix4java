package io.github.danieltuerk.selectrix4java.block;

import io.github.danieltuerk.selectrix4java.AbstractModuleDataDispatcher;

/**
 * Dispatcher for async call of the registered {@link io.github.danieltuerk.selectrix4java.block.BlockListener}s.
 *
 * @param <T> type of the registered {@link BlockListener}
 * @author Daniel Tuerk
 */
public class BlockModuleDataDispatcher<T extends BlockListener> extends AbstractModuleDataDispatcher<T> {

    /**
     * Create new dispatcher without registered listeners.
     */
    public BlockModuleDataDispatcher() {
    }

    /**
     * Inform all listeners that the given block is occupied.
     *
     * @param blockNr number of the block
     */
    public void fireBlockOccupied(final int blockNr) {
        for (T listener : getListeners()) {
            listener.blockOccupied(blockNr);
        }

    }

    /**
     * Inform all listeners that the given block is freed.
     *
     * @param blockNr number of the block
     */
    public void fireBlockFreed(final int blockNr) {
        for (T listener : getListeners()) {
            listener.blockFreed(blockNr);
        }

    }

    /**
     * Inform all listeners about the given block state.
     *
     * @param blockNr number of the block
     * @param state   {@code true} if occupied, {@code false} if freed
     */
    public void fireBlockState(int blockNr, boolean state) {
        if (state) {
            fireBlockOccupied(blockNr);
        } else {
            fireBlockFreed(blockNr);
        }
    }

}
