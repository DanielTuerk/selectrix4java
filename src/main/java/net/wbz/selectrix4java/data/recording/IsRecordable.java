package net.wbz.selectrix4java.data.recording;

import net.wbz.selectrix4java.device.DeviceAccessException;

import java.nio.file.Path;

/**
 * Indicate a {@link net.wbz.selectrix4java.device.Device} as recordable with the
 * {@link BusDataRecorder}.
 *
 * @author Daniel Tuerk
 */
public interface IsRecordable {

    /**
     * Start recording the received data and store the {@link BusDataRecordEntry} in the given
     * destination folder.
     *
     * @param destinationFolder {@link java.nio.file.Path} folder for the record file
     */
    void startRecording(Path destinationFolder) throws DeviceAccessException;

    /**
     * Stop the running recording and return the file path of the record.
     *
     * @return {@link java.nio.file.Path} of the record file
     */
    Path stopRecording() throws DeviceAccessException;

    /**
     * @return {@code true} if recording is running, otherwise {@code false}
     */
    boolean isRecording();

}
