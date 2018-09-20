package net.wbz.selectrix4java.data.recording;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.wbz.selectrix4java.bus.BusDataReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recorder to save the bus data during the recording session and save the recording to the output file. This file can
 * be played back by the {@link BusDataPlayer}.
 * TODO: write immediately by recording with an puffer to avoid {@link java.lang.OutOfMemoryError} for bigger recording
 * sessions
 *
 * @author Daniel Tuerk
 */
public class BusDataRecorder implements BusDataReceiver {

    private static final Logger log = LoggerFactory.getLogger(BusDataRecorder.class);

    /**
     * Record model container to store the bus data during recording. Will be serialized and written to the {@link
     * #outputFile} by calling {@see #getRecordOutput}.
     */
    private BusDataRecord record;

    /**
     * Output file of the last recording.
     */
    private Path outputFile;

    /**
     * State of the recording.
     */
    private boolean running = false;

    /**
     * Start the recording to store the received data by the {@link net.wbz.selectrix4java.bus.BusDataReceiver}
     * interface.
     *
     * @param destinationFolder {@link java.nio.file.Path} for the directory to create the record file and write
     *         the data by calling {@link #getRecordOutput}
     * @throws RecordingException can't record
     */
    public void start(Path destinationFolder) throws RecordingException {
        log.debug("start recording");
        if (!isRunning()) {
            if (!Files.exists(destinationFolder)) {
                try {
                    Files.createDirectory(destinationFolder);
                } catch (IOException e) {
                    throw new RecordingException("can't create diretory for the recording output file");
                }
            }

            if (Files.isDirectory(destinationFolder)) {
                try {
                    outputFile = Files.createFile(
                            Paths.get(destinationFolder.toString(), String.format("record_%d", System.nanoTime())));
                    record = new BusDataRecord();
                    running = true;
                } catch (IOException e) {
                    throw new RecordingException("can't start recording", e);
                }
            } else {
                throw new RecordingException("given destination path is no directory!");
            }
        } else {
            log.warn("didn't start new recording, because recording is already running!");
        }
    }

    /**
     * Stop the actual running recording.
     */
    public void stop() {
        log.debug("stop recording");
        if (isRunning()) {
            running = false;
        } else {
            log.warn("can't stop because recording not running");
        }
    }


    /**
     * @return {@code true} for active recording
     */
    public boolean isRunning() {
        return running;
    }

    @Override
    public void received(int busNr, byte[] data) {
        if (isRunning()) {
            record.addEntry(new BusDataRecordEntry(System.nanoTime(), busNr, data));
        }
    }

    /**
     * Output of the last recording run.
     *
     * @return {@link java.nio.file.Path} file of the last record or {@code null} if no record existing yet
     */
    public Path getRecordOutput() {
        if (!isRunning()) {
            try {
                log.info(String.format("write record entries (%d) to file: %s", record.getEntries().size(),
                        outputFile.toString()));
                Files.write(outputFile, new Gson().toJson(record).getBytes());
                return outputFile;
            } catch (IOException e) {
                log.error("can't write record to file", e);
            }
        }
        return null;
    }
}
