package net.wbz.selectrix4java.bus;

import net.wbz.selectrix4java.AbstractModuleDataDispatcher;

import java.math.BigInteger;

/**
 * Data dispatcher to call the {@link BusListener}.
 *
 * @author Daniel Tuerk
 */
public class BusAddressDataDispatcher extends AbstractModuleDataDispatcher<BusListener> {

    public void fireValueChanged(final int oldValue, final int newValue) {
        for (BusListener listener : getListeners()) {
            if (listener instanceof BusAddressListener) {
                ((BusAddressListener) listener).dataChanged((byte) oldValue, (byte) newValue);
            } else if (listener instanceof final BusAddressBitListener busAddressBitListener) {
                boolean oldBitValue = BigInteger.valueOf(oldValue).testBit(busAddressBitListener.getBitNr() - 1);
                boolean newBitValue = BigInteger.valueOf(newValue).testBit(busAddressBitListener.getBitNr() - 1);

                if (!busAddressBitListener.isCalled() || oldBitValue != newBitValue) {
                    busAddressBitListener.bitChanged(oldBitValue, newBitValue);
                    busAddressBitListener.setCalled(true);
                }
            } else {
                throw new RuntimeException("unknown bus listener instance: " + listener.getClass().getName());
            }

        }
    }
}
