package io.github.danieltuerk.selectrix4java.train;

import io.github.danieltuerk.selectrix4java.AbstractModuleDataDispatcher;

/**
 * Dispatcher for call of the registered {@link io.github.danieltuerk.selectrix4java.train.TrainDataListener}s.
 *
 * @author Daniel Tuerk
 */
public class TrainDataDispatcher extends AbstractModuleDataDispatcher<TrainDataListener> {

    /**
     * Create new dispatcher without registered listeners.
     */
    public TrainDataDispatcher() {
    }

    /**
     * Inform all listeners about the changed driving level.
     *
     * @param level new driving level
     */
    public void fireDrivingLevelChanged(final int level) {
        for (TrainDataListener listener : getListeners()) {
            listener.drivingLevelChanged(level);
        }
    }

    /**
     * Inform all listeners about the changed driving direction.
     *
     * @param direction new driving direction
     */
    public void fireDrivingDirectionChanged(final TrainModule.DRIVING_DIRECTION direction) {
        for (TrainDataListener listener : getListeners()) {
            listener.drivingDirectionChanged(direction);
        }
    }

    /**
     * Inform all listeners about the changed function state.
     *
     * @param address     address of the function decoder
     * @param functionBit bit number of the function (1-8)
     * @param state       new state
     */
    public void fireFunctionStateChanged(final int address, final int functionBit, final boolean state) {
        for (TrainDataListener listener : getListeners()) {
            listener.functionStateChanged(address, functionBit, state);
        }
    }

    /**
     * Inform all listeners about the changed light state.
     *
     * @param on new state
     */
    public void fireLightStateChanged(final boolean on) {
        for (TrainDataListener listener : getListeners()) {
            listener.lightStateChanged(on);
        }
    }

    /**
     * Inform all listeners about the changed horn state.
     *
     * @param on new state
     */
    public void fireHornStateChanged(final boolean on) {
        for (TrainDataListener listener : getListeners()) {
            listener.hornStateChanged(on);
        }
    }
}
