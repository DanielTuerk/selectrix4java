package net.wbz.selectrix4java.train;

import com.google.common.collect.Lists;
import net.wbz.selectrix4java.Module;
import net.wbz.selectrix4java.bus.BusAddress;
import net.wbz.selectrix4java.bus.BusAddressListener;

import java.math.BigInteger;
import java.util.List;

/**
 * This module is an wrapper for {@link net.wbz.selectrix4java.bus.BusAddress}s
 * from an function decoder of an train.
 * <p/>
 * The train can have several addresses for multiple decoders.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class TrainModule implements Module
{

  /**
   * Light of the train: bit 6
   */
  public static final int BIT_DRIVING_DIRECTION = 6;

  /**
   * Light of the train: bit 7
   */
  public static final int BIT_LIGHT = 7;

  /**
   * Light of the train: bit 8
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
   * Create a new module with the main address and additional function addresses.
   *
   * @param address             {@link net.wbz.selectrix4java.bus.BusAddress}
   * @param additionalAddresses additional addresses (e.g. function decoder)
   */
  public TrainModule(BusAddress address, BusAddress... additionalAddresses)
  {
    this.address = address;
    address.addListener(new BusAddressListener()
    {
      @Override
      public void dataChanged(byte oldValue, byte newValue)
      {
        BigInteger wrappedOldValue = BigInteger.valueOf(oldValue);
        BigInteger wrappedNewValue = BigInteger.valueOf(newValue);

        // direction
        if (wrappedOldValue.testBit(BIT_DRIVING_DIRECTION - 1)
            != wrappedNewValue.testBit(BIT_DRIVING_DIRECTION - 1))
        {
          dispatcher.fireDrivingDirectionChanged(wrappedNewValue.testBit(BIT_DRIVING_DIRECTION - 1) ?
              DRIVING_DIRECTION.FORWARD :
              DRIVING_DIRECTION.BACKWARD);
        }
        // light
        if (wrappedOldValue.testBit(BIT_LIGHT - 1)
            != wrappedNewValue.testBit(BIT_LIGHT - 1))
        {
          dispatcher.fireLightStateChanged(wrappedNewValue.testBit(BIT_LIGHT - 1));
        }
        // horn
        if (wrappedOldValue.testBit(BIT_HORN - 1)
            != wrappedNewValue.testBit(BIT_HORN - 1))
        {
          dispatcher.fireHornStateChanged(wrappedNewValue.testBit(BIT_HORN - 1));
        }
        // speed: check for changes in bit 1-5
        int oldDrivingLevel = wrappedOldValue.clearBit(5).clearBit(6).clearBit(7).intValue() & 0xff;
        int newDrivingLevel = wrappedNewValue.clearBit(5).clearBit(6).clearBit(7).intValue() & 0xff;
        if (oldDrivingLevel != newDrivingLevel) {
          dispatcher.fireDrivingLevelChanged(newDrivingLevel);
        }
      }
    });
    this.additionalAddresses = Lists.newArrayList(additionalAddresses);
    for (final BusAddress additionalAddress : additionalAddresses)
    {
      additionalAddress.addListener(new BusAddressListener()
      {
        @Override
        public void dataChanged(byte oldValue, byte newValue)
        {
          for (int i = 1; i < 9; i++)
          {
            boolean functionState = BigInteger.valueOf(oldValue).testBit(i) != BigInteger.valueOf(newValue).testBit(i);
            dispatcher.fireFunctionStateChanged(additionalAddress.getAddress(), i, functionState);
          }
        }
      });
    }
  }

  /**
   * Change the driving level.
   *
   * @param level target
   * @return {@link net.wbz.selectrix4java.train.TrainModule}
   */
  public TrainModule setDrivingLevel(int level)
  {
    if (level >= 0 && level <= 31)
    {
      // bit 1-5

      BigInteger wrappedLevel = BigInteger.valueOf(level);
      for (int i = 0; i < 5; i++)
      {

        if (wrappedLevel.testBit(i))
        {
          getAddress().setBit(i + 1);
        }
        else
        {
          getAddress().clearBit(i + 1);
        }
      }
      address.sendData((byte) (address.getData() | level));
    }
    return this;
  }

  public enum DRIVING_DIRECTION
  {
    FORWARD, BACKWARD
  }

  /**
   * Change the driving direction.
   *
   * @param direction {@link net.wbz.selectrix4java.train.TrainModule.DRIVING_DIRECTION}
   * @return {@link net.wbz.selectrix4java.train.TrainModule}
   */
  public TrainModule setDirection(DRIVING_DIRECTION direction)
  {
    switch (direction)
    {
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
  public TrainModule setLight(boolean state)
  {
    if (state)
    {
      address.setBit(BIT_LIGHT);
    }
    else
    {
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
  public TrainModule setHorn(boolean state)
  {
    if (state)
    {
      address.setBit(BIT_HORN);
    }
    else
    {
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
  public void addTrainDataListener(TrainDataListener listener)
  {
    dispatcher.addListener(listener);
  }

  /**
   * Remove existing listener.
   *
   * @param listener {@link net.wbz.selectrix4java.train.TrainDataListener}
   */
  public void removeTrainDataListener(TrainDataListener listener)
  {
    dispatcher.removeListener(listener);
  }

  /**
   * Corresponding address of the train.
   *
   * @return {@link net.wbz.selectrix4java.bus.BusAddress}
   */
  public BusAddress getAddress()
  {
    return address;
  }

  /**
   * Additional addresses of functions for the train.
   *
   * @return {@link List<net.wbz.selectrix4java.bus.BusAddress>}
   */
  public List<BusAddress> getAdditionalAddresses()
  {
    return additionalAddresses;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }
    if (o == null || getClass() != o.getClass())
    {
      return false;
    }

    TrainModule that = (TrainModule) o;

    return address.equals(that.address);
  }

  @Override
  public int hashCode()
  {
    return address.hashCode();
  }
}
