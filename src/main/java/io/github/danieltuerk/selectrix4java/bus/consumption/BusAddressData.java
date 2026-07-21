package io.github.danieltuerk.selectrix4java.bus.consumption;

/**
 * Data holder for {@link BusMultiAddressDataConsumer}.
 *
 * @author Daniel Tuerk
 */
public record BusAddressData(int bus, int address, int oldDataValue, int newDataValue) {

}
