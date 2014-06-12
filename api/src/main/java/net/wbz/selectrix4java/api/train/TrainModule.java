package net.wbz.selectrix4java.api.train;

import com.google.common.collect.Lists;
import net.wbz.selectrix4java.api.Module;
import net.wbz.selectrix4java.api.bus.BusAddress;
import net.wbz.selectrix4java.api.bus.BusAddressListener;

import java.math.BigInteger;
import java.util.List;

/**
 * This module is an wrapper for {@link net.wbz.selectrix4java.api.bus.BusAddress}s
 * from an function decoder of an train.
 *
 * The train can have several addresses for multiple decoders.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class TrainModule implements Module {

    /**
     * Light of the train: bit 6
     */
    public static final int BIT_DRIVING_DIRECTION = 5;

    /**
     * Light of the train: bit 7
     */
    public static final int BIT_LIGHT = 6;

    /**
     * Light of the train: bit 8
     */
    public static final int BIT_HORN = 7;

    /**
     * Main address of the train.
     */
    private final BusAddress address;

    /**
     * Additional function addresses of the train or even an second decoder.
     */
    private final List<BusAddress> additionalAddresses;

    /**
     * Dispatcher to fire asynchronous the train events to the listeners.
     */
    private final TrainDataDispatcher dispatcher = new TrainDataDispatcher();

    /**
     * Create a new module with the main address and additional function addresses.
     *
     * @param address             {@link net.wbz.selectrix4java.api.bus.BusAddress}
     * @param additionalAddresses additional addresses (e.g. function decoder)
     */
    public TrainModule(BusAddress address, BusAddress... additionalAddresses) {
        this.address = address;
        address.addListener(new BusAddressListener() {
            @Override
            public void dataChanged(byte oldValue, byte newValue) {
                BigInteger wrappedOldValue = BigInteger.valueOf(oldValue);
                BigInteger wrappedNewValue = BigInteger.valueOf(newValue);

                // direction
                if (wrappedOldValue.testBit(BIT_DRIVING_DIRECTION)
                        != wrappedNewValue.testBit(BIT_DRIVING_DIRECTION)) {
                    dispatcher.fireDrivingDirectionChanged(wrappedNewValue.testBit(BIT_DRIVING_DIRECTION) ? DRIVING_DIRECTION.FORWARD : DRIVING_DIRECTION.BACKWARD);
                }
                // light
                if (wrappedOldValue.testBit(BIT_LIGHT)
                        != wrappedNewValue.testBit(BIT_LIGHT)) {
                    dispatcher.fireLightStateChanged(wrappedNewValue.testBit(BIT_LIGHT));
                }
                // horn
                if (wrappedOldValue.testBit(BIT_HORN)
                        != wrappedNewValue.testBit(BIT_HORN)) {
                    dispatcher.fireHornStateChanged(wrappedNewValue.testBit(BIT_HORN));
                }
                // speed: check for changes in bit 1-5
                if (wrappedOldValue.testBit(0) != wrappedNewValue.testBit(0)
                        && wrappedOldValue.testBit(1) != wrappedNewValue.testBit(1)
                        && wrappedOldValue.testBit(2) != wrappedNewValue.testBit(2)
                        && wrappedOldValue.testBit(3) != wrappedNewValue.testBit(3)
                        && wrappedOldValue.testBit(4) != wrappedNewValue.testBit(4)) {
                    //use bit 1-5 of copy as the driving level
                    dispatcher.fireDrivingLevelChanged(wrappedNewValue.clearBit(5).clearBit(6).clearBit(7).intValue());
                }
            }
        });
        this.additionalAddresses = Lists.newArrayList(additionalAddresses);
        for (final BusAddress additionalAddress : additionalAddresses) {
            additionalAddress.addListener(new BusAddressListener() {
                @Override
                public void dataChanged(byte oldValue, byte newValue) {
                    //TODO
                    int functionBit = 1;
                    //oldValue & newValue
                    boolean functionState = true;
                    dispatcher.fireFunctionStateChanged(additionalAddress.getAddress(), functionBit, functionState);
                }
            });
        }
    }

    /**
     * Change the driving level.
     *
     * @param level target
     * @return {@link net.wbz.selectrix4java.api.train.TrainModule}
     */
    public TrainModule setDrivingLevel(int level) {
        if (level >= 0 && level <= 63) {
            // bit 1-5
            address.sendData((byte) (address.getData() | level));
        }
        return this;
    }

    public enum DRIVING_DIRECTION {FORWARD, BACKWARD}

    /**
     * Change the driving direction.
     *
     * @param direction {@link net.wbz.selectrix4java.api.train.TrainModule.DRIVING_DIRECTION}
     * @return {@link net.wbz.selectrix4java.api.train.TrainModule}
     */
    public TrainModule setDirection(DRIVING_DIRECTION direction) {
        switch (direction) {
            case FORWARD:
                address.setBit(BIT_DRIVING_DIRECTION);
                break;
            case BACKWARD:
                address.clearBit(BIT_DRIVING_DIRECTION);
                break;
        }
        address.send();
        return this;
    }

    /**
     * Turn light on or off.
     *
     * @param state light state
     * @return {@link net.wbz.selectrix4java.api.train.TrainModule}
     */
    public TrainModule setLight(boolean state) {
        if (state) {
            address.setBit(BIT_LIGHT);
        } else {
            address.clearBit(BIT_LIGHT);
        }
        address.send();
        return this;
    }

    /**
     * Turn horn on or off.
     *
     * @param state horn state
     * @return {@link net.wbz.selectrix4java.api.train.TrainModule}
     */
    public TrainModule setHorn(boolean state) {
        if (state) {
            address.setBit(BIT_HORN);
        } else {
            address.clearBit(BIT_HORN);
        }
        address.send();
        return this;
    }


    /**
     * Add listener to receive state changes of the train.
     *
     * @param listener {@link net.wbz.selectrix4java.api.train.TrainDataListener}
     */
    public void addTrainDataListener(TrainDataListener listener) {
        dispatcher.addListener(listener);
    }

    /**
     * Remove existing listener.
     *
     * @param listener {@link net.wbz.selectrix4java.api.train.TrainDataListener}
     */
    public void removeTrainDataListener(TrainDataListener listener) {
        dispatcher.removeListener(listener);
    }


    /**
     * Corresponding address of the train.
     *
     * @return {@link net.wbz.selectrix4java.api.bus.BusAddress}
     */
    public BusAddress getAddress() {
        return address;
    }

    /**
     * Additional addresses of functions for the train.
     *
     * @return {@link List<net.wbz.selectrix4java.api.bus.BusAddress>}
     */
    public List<BusAddress> getAdditionalAddresses() {
        return additionalAddresses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrainModule that = (TrainModule) o;

        return address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }
}
