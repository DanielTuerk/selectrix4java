package io.github.danieltuerk.selectrix4java.data.recording;

/**
 * Model to store the bus data which is recorded by the {@link BusDataRecorder} and played by {@link BusDataPlayer}.
 *
 * @author Daniel Tuerk
 */
public record BusDataRecordEntry(long timestamp, int bus, byte[] data) {

}
