package net.wbz.selectrix4java.train;

/**
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public interface TrainDataListener {

    void drivingLevelChanged(int level);
    void drivingDirectionChanged(TrainModule.DRIVING_DIRECTION direction);
    void functionStateChanged(int address, int functionBit, boolean state);
    void lightStateChanged(boolean on);
    void hornStateChanged(boolean on);

}
