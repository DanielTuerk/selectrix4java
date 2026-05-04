package net.wbz.selectrix4java.data.recording;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for a recording session of the {@link BusDataRecorder} which can be played back by the {@link
 * BusDataPlayer}.
 *
 * Each single entry representing an bus value change of the buses.
 *
 * @author Daniel Tuerk
 */
public class BusDataRecord {

    private final List<BusDataRecordEntry> entries = new ArrayList<>();

    public void addEntry(BusDataRecordEntry entry) {
        entries.add(entry);
    }

    public List<BusDataRecordEntry> getEntries() {
        return entries;
    }
}
