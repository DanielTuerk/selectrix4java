package io.github.danieltuerk.selectrix4java.bus.consumption;

/**
 * Data holder for {@link BusMultiAddressDataConsumer}.
 *
 * @param bus bus number
 * @param address address on the bus
 * @param oldDataValue previous data value
 * @param newDataValue new data value
 * @author Daniel Tuerk
 */
public record BusAddressData(int bus, int address, int oldDataValue, int newDataValue) {

}
