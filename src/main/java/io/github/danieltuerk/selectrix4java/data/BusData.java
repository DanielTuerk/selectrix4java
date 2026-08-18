package io.github.danieltuerk.selectrix4java.data;

/**
 * Data to send to the bus.
 *
 * @param bus bus number
 * @param address address on the bus
 * @param data value to send
 * @author Daniel Tuerk
 */
public record BusData(int bus, int address, int data) {
}
