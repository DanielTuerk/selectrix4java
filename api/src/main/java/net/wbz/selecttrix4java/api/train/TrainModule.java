package net.wbz.selecttrix4java.api.train;

import com.google.common.collect.Lists;
import net.wbz.selecttrix4java.api.bus.BusAddress;
import net.wbz.selecttrix4java.api.bus.BusAddressListener;

import java.math.BigInteger;
import java.util.List;

/**
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class TrainModule {

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
     * @param address             {@link net.wbz.selecttrix4java.api.bus.BusAddress}
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
                    dispatcher.fireLightStateChanged(wrappedNewValue.testBit(BIT_HORN));
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

    public TrainModule setDrivingLevel(int level) {
        if (level >= 0 && level <= 63) {
            // bit 1-5
            address.sendData((byte) (address.getData() | level));
        }
        return this;
    }

    public enum DRIVING_DIRECTION {FORWARD, BACKWARD}

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

    public TrainModule setLight(boolean state) {
        if (state) {
            address.setBit(BIT_LIGHT);
        } else {
            address.clearBit(BIT_LIGHT);
        }
        address.send();
        return this;
    }

    public TrainModule setHorn(boolean state) {
        if (state) {
            address.setBit(BIT_HORN);
        } else {
            address.clearBit(BIT_HORN);
        }
        address.send();
        return this;
    }


    public void addTrainDataListener(TrainDataListener listener) {
        dispatcher.addTrainDataListener(listener);
    }

    public void removeTrainDataListener(TrainDataListener listener) {
        dispatcher.removeTrainDataListener(listener);
    }


    public BusAddress getAddress() {
        return address;
    }

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
