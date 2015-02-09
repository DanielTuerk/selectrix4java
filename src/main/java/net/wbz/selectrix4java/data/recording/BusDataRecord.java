package net.wbz.selectrix4java.data.recording;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Container for an recording session of the {@link BusDataRecorder} which can be played
 * back by the {@link BusDataPlayer}.
 *
 * Each single entry representing an bus value change of the buses.
 *
 * @author Daniel Tuerk
 */
public class BusDataRecord {
    private final List<BusDataRecordEntry> entries = Lists.newArrayList();

    public void addEntry( BusDataRecordEntry entry){
        entries.add(entry);
    }

    public List<BusDataRecordEntry> getEntries() {
        return entries;
    }
}
