package io.github.danieltuerk.selectrix4java.bus;

/**
 * Listener to observe a single bit of a {@link io.github.danieltuerk.selectrix4java.bus.BusAddress}.
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

    /**
     * Observed bit number.
     *
     * @return bit number
     */
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
     * State of the listener.
     *
     * @return {@code true} if the listener was called since construction
     */
    public boolean isCalled() {
        return called;
    }

    /**
     * Set whether the listener was called since construction.
     *
     * @param called new state
     */
    public void setCalled(boolean called) {
        this.called = called;
    }
}
