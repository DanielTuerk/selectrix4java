package net.wbz.selecttrix4java.api.train;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class TrainDataDispatcher {

    private final Queue<TrainDataListener> listeners = new ConcurrentLinkedQueue<>();
    private final ExecutorService executorService;

    public TrainDataDispatcher() {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("train-data-dispatcher-%d").build();
        executorService = Executors.newCachedThreadPool(namedThreadFactory);
    }

    public void addTrainDataListener(TrainDataListener listener) {
        listeners.add(listener);
    }


    public void removeTrainDataListener(TrainDataListener listener) {
        listeners.remove(listener);
    }

    public void fireDrivingLevelChanged(final int level) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                for (TrainDataListener listener : listeners) {
                    listener.drivingLevelChanged(level);
                }
            }
        });
    }

    public void fireDrivingDirectionChanged(final TrainModule.DRIVING_DIRECTION direction) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                for (TrainDataListener listener : listeners) {
                    listener.drivingDirectionChanged(direction);
                }
            }
        });

    }

    public void fireFunctionStateChanged(byte address, int functionBit, boolean state) {
        throw new RuntimeException();
    }

    public void fireLightStateChanged(final boolean on) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                for (TrainDataListener listener : listeners) {
                    listener.lightStateChanged(on);
                }
            }
        });
    }

    public void fireHornStateChanged(final boolean on) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                for (TrainDataListener listener : listeners) {
                    listener.hornStateChanged(on);
                }
            }
        });
    }
}
