package net.wbz.selectrix4java.data.recording;

/**
 * Listener for the playback state of the {@link net.wbz.selectrix4java.data.recording.BusDataPlayer}.
 *
 * @author Daniel Tuerk
 */
public interface BusDataPlayerListener {

    public void playbackStarted();

    public void playbackStopped();
}
