package net.wbz.selectrix4java.api.block;

/**
 * Listener for state changes of an {@link net.wbz.selectrix4java.api.block.BlockModule}.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public interface BlockListener {

    public void blockOccupied(int blockNr);
    public void blockFreed(int blockNr);

}
