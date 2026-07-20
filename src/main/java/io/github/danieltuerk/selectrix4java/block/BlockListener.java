package io.github.danieltuerk.selectrix4java.block;

/**
 * Listener for state changes of an {@link io.github.danieltuerk.selectrix4java.block.BlockModule}.
 *
 * @author Daniel Tuerk
 */
public interface BlockListener {

    void blockOccupied(int blockNr);

    void blockFreed(int blockNr);

}
