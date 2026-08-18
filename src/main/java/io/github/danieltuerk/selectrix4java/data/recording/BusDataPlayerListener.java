package io.github.danieltuerk.selectrix4java.data.recording;

/**
 * Listener for the playback state of the {@link io.github.danieltuerk.selectrix4java.data.recording.BusDataPlayer}.
 *
 * @author Daniel Tuerk
 */
public interface BusDataPlayerListener {

    /**
     * Playback has started.
     */
    void playbackStarted();

    /**
     * Playback has stopped.
     */
    void playbackStopped();
}
