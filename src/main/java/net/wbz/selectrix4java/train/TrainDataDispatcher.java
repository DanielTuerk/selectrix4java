package net.wbz.selectrix4java.train;

import net.wbz.selectrix4java.AbstractModuleDataDispatcher;

/**
 * Dispatcher for async call of the registered {@link net.wbz.selectrix4java.train.TrainDataListener}s.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class TrainDataDispatcher extends AbstractModuleDataDispatcher<TrainDataListener> {

    public void fireDrivingLevelChanged(final int level) {
        fire(new ListenerRunnable() {
            @Override
            public void run() {
                getListener().drivingLevelChanged(level);
            }
        });
    }

    public void fireDrivingDirectionChanged(final TrainModule.DRIVING_DIRECTION direction) {
        fire(new ListenerRunnable() {
            @Override
            public void run() {
                getListener().drivingDirectionChanged(direction);
            }
        });
    }

    public void fireFunctionStateChanged(byte address, int functionBit, boolean state) {
        //TODO
        throw new RuntimeException();
    }

    public void fireLightStateChanged(final boolean on) {
        fire(new ListenerRunnable() {
            @Override
            public void run() {
                getListener().lightStateChanged(on);
            }
        });
    }

    public void fireHornStateChanged(final boolean on) {
        fire(new ListenerRunnable() {
            @Override
            public void run() {
                getListener().hornStateChanged(on);
            }
        });
    }
}
