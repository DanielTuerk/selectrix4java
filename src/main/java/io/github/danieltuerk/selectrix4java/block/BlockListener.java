package io.github.danieltuerk.selectrix4java.block;

/**
 * Listener for state changes of an {@link io.github.danieltuerk.selectrix4java.block.BlockModule}.
 *
 * @author Daniel Tuerk
 */
public interface BlockListener {

    /**
     * The given block is occupied.
     *
     * @param blockNr number of the block
     */
    void blockOccupied(int blockNr);

    /**
     * The given block is freed.
     *
     * @param blockNr number of the block
     */
    void blockFreed(int blockNr);

}
