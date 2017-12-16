package net.wbz.selectrix4java.train;

import java.math.BigInteger;
import java.util.List;

import com.google.common.collect.Lists;

import net.wbz.selectrix4java.Module;
import net.wbz.selectrix4java.bus.BusAddress;
import net.wbz.selectrix4java.bus.BusAddressListener;
import net.wbz.selectrix4java.bus.consumption.AbstractBusDataConsumer;

/**
 * This module is an wrapper for {@link net.wbz.selectrix4java.bus.BusAddress}s
 * from an function decoder of an train.
 * <p/>
 * The train can have several addresses for multiple decoders.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class TrainModule implements Module {

    /**
     * Driving direction of the train: bit 6
     */
    public static final int BIT_DRIVING_DIRECTION = 6;

    /**
     * Light of the train: bit 7
     */
    public static final int BIT_LIGHT = 7;

    /**
     * Horn of the train: bit 8
     */
    public static final int BIT_HORN = 8;
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
     * Last received driving level.
     */
    private int lastDrivingLevel = 0;

    /**
     * Create a new module with the main address and additional function addresses.
     *
     * @param address {@link net.wbz.selectrix4java.bus.BusAddress}
     * @param additionalAddresses additional addresses (e.g. function decoder)
     */
    public TrainModule(BusAddress address, BusAddress... additionalAddresses) {
        this.address = address;
        address.addListener(new BusAddressListener() {
            private boolean initialCall = true;

            @Override
            public void dataChanged(byte oldValue, byte newValue) {
                BigInteger wrappedOldValue = BigInteger.valueOf(oldValue);
                BigInteger wrappedNewValue = BigInteger.valueOf(newValue);

                // direction
                if (initialCall || wrappedOldValue.testBit(BIT_DRIVING_DIRECTION - 1) != wrappedNewValue.testBit(
                        BIT_DRIVING_DIRECTION
                                - 1)) {
                    dispatcher.fireDrivingDirectionChanged(wrappedNewValue.testBit(BIT_DRIVING_DIRECTION - 1)
                            ? DRIVING_DIRECTION.FORWARD : DRIVING_DIRECTION.BACKWARD);
                }
                // light
                if (initialCall || wrappedOldValue.testBit(BIT_LIGHT - 1) != wrappedNewValue.testBit(BIT_LIGHT - 1)) {
                    dispatcher.fireLightStateChanged(wrappedNewValue.testBit(BIT_LIGHT - 1));
                }
                // horn
                if (initialCall || wrappedOldValue.testBit(BIT_HORN - 1) != wrappedNewValue.testBit(BIT_HORN - 1)) {
                    dispatcher.fireHornStateChanged(wrappedNewValue.testBit(BIT_HORN - 1));
                }
                // speed: check for changes in bit 1-5
                int newDrivingLevel = wrappedNewValue.clearBit(5).clearBit(6).clearBit(7).intValue() & 0xff;
                if (initialCall || lastDrivingLevel != newDrivingLevel) {
                    dispatcher.fireDrivingLevelChanged(newDrivingLevel);
                }
                lastDrivingLevel = newDrivingLevel;
                initialCall = false;
            }
        });
        this.additionalAddresses = Lists.newArrayList(additionalAddresses);
        for (final BusAddress additionalAddress : additionalAddresses) {
            registerAdditionalAddress(additionalAddress);
        }
    }

    private void registerAdditionalAddress(final BusAddress additionalAddress) {
        additionalAddress.addListener(new BusAddressListener() {
            private boolean initialCall = true;

            @Override
            public void dataChanged(byte oldValue, byte newValue) {
                for (int i = 1; i < 9; i++) {
                    boolean newBitState = BigInteger.valueOf(newValue).testBit(i);
                    if (initialCall || BigInteger.valueOf(oldValue).testBit(i) != newBitState) {
                        dispatcher.fireFunctionStateChanged(additionalAddress.getAddress(), i, newBitState);
                    }
                }
                initialCall = false;
            }
        });
    }

    public void setFunctionState(BusAddress address, int bit, boolean state) {
        if (!additionalAddresses.contains(address)) {
            additionalAddresses.add(address);
            registerAdditionalAddress(address);
        }
        if (state) {
            address.setBit(bit);
        } else {
            address.clearBit(bit);
        }
        address.send();
    }

    /**
     * Change the driving level.
     *
     * @param level target
     * @return {@link net.wbz.selectrix4java.train.TrainModule}
     */
    public TrainModule setDrivingLevel(int level) {
        // avoid duplicate sending
        if (level != lastDrivingLevel) {
            if (level >= 0 && level <= 31) {
                // bit 1-5
                BigInteger wrappedLevel = BigInteger.valueOf(level);
                for (int i = 0; i < 5; i++) {
                    if (wrappedLevel.testBit(i)) {
                        address.setBit(i + 1);
                    } else {
                        address.clearBit(i + 1);
                    }
                }
                address.send();
            }
        }
        return this;
    }

    /**
     * Change the driving direction.
     *
     * @param direction {@link net.wbz.selectrix4java.train.TrainModule.DRIVING_DIRECTION}
     * @return {@link net.wbz.selectrix4java.train.TrainModule}
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
     * @return {@link net.wbz.selectrix4java.train.TrainModule}
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
     * @return {@link net.wbz.selectrix4java.train.TrainModule}
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
     * @param listener {@link net.wbz.selectrix4java.train.TrainDataListener}
     */
    public void addTrainDataListener(TrainDataListener listener) {
        dispatcher.addListener(listener);
    }

    /**
     * Remove existing listener.
     *
     * @param listener {@link net.wbz.selectrix4java.train.TrainDataListener}
     */
    public void removeTrainDataListener(TrainDataListener listener) {
        dispatcher.removeListener(listener);
    }

    /**
     * Remove all existing listeners.
     */
    public void removeAllTrainDataListeners() {
        dispatcher.removeAllListeners();
    }

    @Override
    public int getBus() {
        return address.getBus();
    }

    /**
     * Corresponding address of the train.
     *
     * @return {@link net.wbz.selectrix4java.bus.BusAddress}
     */
    public int getAddress() {
        return address.getAddress();
    }

    @Override
    public BusAddress getBusAddress() {
        return address;
    }

    @Override
    public List<AbstractBusDataConsumer> getConsumers() {
        return null;
    }

    /**
     * Additional addresses of functions for the train.
     *
     * @return {@link List<net.wbz.selectrix4java.bus.BusAddress>}
     */
    public List<BusAddress> getAdditionalAddresses() {
        return additionalAddresses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TrainModule that = (TrainModule) o;

        return address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    /**
     * Driving direction of the train.
     */
    public enum DRIVING_DIRECTION {
        FORWARD, BACKWARD
    }
}
