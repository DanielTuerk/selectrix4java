package io.github.danieltuerk.selectrix4java.block;

/**
 * {@link BlockListener} which only listen to one defined block number.
 *
 * @author Daniel Tuerk
 */
public abstract class BlockNumberListener implements BlockListener {

    private final int blockNumber;

    /**
     * Create listener for the given block number.
     *
     * @param blockNumber number of the observed block
     */
    public BlockNumberListener(int blockNumber) {
        this.blockNumber = blockNumber;
    }

    @Override
    public void blockOccupied(int blockNr) {
        if (blockNr == blockNumber) {
            occupied();
        }
    }

    @Override
    public void blockFreed(int blockNr) {
        if (blockNr == blockNumber) {
            freed();
        }
    }

    /**
     * Block is freed.
     */
    public abstract void freed();

    /**
     * Block is occupied.
     */
    public abstract void occupied();
}
