package net.wbz.selectrix4java.bus;

/**
 * Listener to observe a single bit of a {@link net.wbz.selectrix4java.bus.BusAddress}.
 *
 * @author Daniel Tuerk
 */
abstract public class BusAddressBitListener implements BusListener {

    private final int bitNr;
    private boolean called = false;

    /**
     * Create new listener for the given bit number.
     *
     * @param bitNr 1-8
     */
    public BusAddressBitListener(int bitNr) {
        this.bitNr = bitNr;
    }

    public int getBitNr() {
        return bitNr;
    }

    /**
     * State of the bit has changed.
     *
     * @param oldValue {@link java.lang.Boolean}
     * @param newValue {@link java.lang.Boolean}
     */
    abstract public void bitChanged(boolean oldValue, boolean newValue);

    /**
     * @return {@code true} if the listener was called since construction
     */
    public boolean isCalled() {
        return called;
    }

    public void setCalled(boolean called) {
        this.called = called;
    }
}
