package net.wbz.selecttrix4java.api.train;

/**
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public interface TrainDataListener {

    public void drivingLevelChanged(int level);
    public void drivingDirectionChanged(TrainModule.DRIVING_DIRECTION direction);
    public void functionStateChanged(byte address, int functionBit, boolean state);
    public void lightStateChanged(boolean on);
    public void hornStateChanged(boolean on);

}
