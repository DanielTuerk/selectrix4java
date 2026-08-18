package io.github.danieltuerk.selectrix4java.data.recording;

/**
 * Model to store the bus data which is recorded by the {@link BusDataRecorder} and played by {@link BusDataPlayer}.
 *
 * @param timestamp time of the recorded value in nanoseconds
 * @param bus bus number
 * @param data recorded bus data
 * @author Daniel Tuerk
 */
public record BusDataRecordEntry(long timestamp, int bus, byte[] data) {

}
