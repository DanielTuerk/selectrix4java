package io.github.danieltuerk.selectrix4java.train;

/**
 * Listener for state changes of a {@link io.github.danieltuerk.selectrix4java.train.TrainModule}.
 *
 * @author Daniel Tuerk
 */
public interface TrainDataListener {

    /**
     * The driving level has changed.
     *
     * @param level new driving level
     */
    void drivingLevelChanged(int level);

    /**
     * The driving direction has changed.
     *
     * @param direction new driving direction
     */
    void drivingDirectionChanged(TrainModule.DRIVING_DIRECTION direction);

    /**
     * The state of a function has changed.
     *
     * @param address     address of the function decoder
     * @param functionBit bit number of the function (1-8)
     * @param state       new state
     */
    void functionStateChanged(int address, int functionBit, boolean state);

    /**
     * The light state has changed.
     *
     * @param on new state
     */
    void lightStateChanged(boolean on);

    /**
     * The horn state has changed.
     *
     * @param on new state
     */
    void hornStateChanged(boolean on);

}
