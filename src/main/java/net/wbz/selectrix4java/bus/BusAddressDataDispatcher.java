package net.wbz.selectrix4java.bus;

import java.math.BigInteger;

import net.wbz.selectrix4java.AbstractModuleDataDispatcher;

/**
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class BusAddressDataDispatcher extends AbstractModuleDataDispatcher<BusListener> {

    public void fireValueChanged(final int oldValue, final int newValue) {
        for (BusListener listener : getListeners()) {

            // fire(new ListenerRunnable() {
            // @Override
            // public void run() {

            // BusListener listener = getListener();
            if (listener instanceof BusAddressListener) {
                ((BusAddressListener) listener).dataChanged((byte) oldValue, (byte) newValue);
            } else if (listener instanceof BusAddressBitListener) {
                BusAddressBitListener busAddressBitListener = (BusAddressBitListener) listener;
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
        // });
    }
}
