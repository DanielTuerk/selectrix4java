package net.wbz.selectrix4java.train;

import net.wbz.selectrix4java.AbstractModuleDataDispatcher;

/**
 * Dispatcher for async call of the registered {@link net.wbz.selectrix4java.train.TrainDataListener}s.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class TrainDataDispatcher extends AbstractModuleDataDispatcher<TrainDataListener> {

    public void fireDrivingLevelChanged(final int level) {
        for (TrainDataListener listener : getListeners()) {
            listener.drivingLevelChanged(level);
        }

        // fire(new ListenerRunnable() {
        // @Override
        // public void run() {
        // listener.drivingLevelChanged(level);
        // }
        // });
    }

    public void fireDrivingDirectionChanged(final TrainModule.DRIVING_DIRECTION direction) {
        for (TrainDataListener listener : getListeners()) {
            listener.drivingDirectionChanged(direction);
        }
        // fire(new ListenerRunnable() {
        // @Override
        // public void run() {
        // listener.drivingDirectionChanged(direction);
        // }
        // });
    }

    public void fireFunctionStateChanged(final int address, final int functionBit, final boolean state) {
        for (TrainDataListener listener : getListeners()) {
            listener.functionStateChanged(address, functionBit, state);
        }
        // fire(new ListenerRunnable() {
        // @Override
        // public void run() {
        // listener.functionStateChanged(address, functionBit, state);
        // }
        // });
    }

    public void fireLightStateChanged(final boolean on) {
        for (TrainDataListener listener : getListeners()) {
            listener.lightStateChanged(on);
        }
        // fire(new ListenerRunnable() {
        // @Override
        // public void run() {
        // listener.lightStateChanged(on);
        // }
        // });
    }

    public void fireHornStateChanged(final boolean on) {
        for (TrainDataListener listener : getListeners()) {
            listener.hornStateChanged(on);
        }
        // fire(new ListenerRunnable() {
        // @Override
        // public void run() {
        // listener.hornStateChanged(on);
        // }
        // });
    }
}
