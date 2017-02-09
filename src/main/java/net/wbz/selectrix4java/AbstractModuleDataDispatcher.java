package net.wbz.selectrix4java;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Dispatcher for async calls of the registered listeners.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class AbstractModuleDataDispatcher<T> {

    private final Collection<T> listeners = new ArrayList<>();
    private final ExecutorService executorService;

    public AbstractModuleDataDispatcher() {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat(
                this.getClass().getSimpleName() + "-data-dispatcher-%d").build();
        // TODO synchronous or asynchronous?
        executorService = Executors.newSingleThreadExecutor(namedThreadFactory);
    }

    public void addListener(T listener) {
        listeners.add(listener);

        // TODO fire: all last runnables which have to be stored here
    }

    public void removeListener(T listener) {
        listeners.remove(listener);
    }

    public void removeAllListeners() {
        listeners.clear();
    }

    protected Collection<T> getListeners() {
        return listeners;
    }
    // /**
    // * TODO doc
    // * @param listener
    // * @param runnable
    // */
    // protected synchronized void fire(T listener,ListenerRunnable runnable) {
    // runnable.setListener(listener);
    // executorService.submit(runnable);
    // }
    //
    // /**
    // * TODO: multi usage of one runnable possible?
    // *
    // * Execute the given runnable for all registered listeners.
    // *
    // * @param runnable {@link AbstractModuleDataDispatcher.ListenerRunnable}
    // */
    // protected synchronized void fire(ListenerRunnable runnable) {
    // for (T listener : listeners) {
    // fire(listener,runnable);
    // }
    // }

    /**
     * Runnable with listener instance to perform an async listener call.
     * Is used by the implementing class of {@link AbstractModuleDataDispatcher} to
     * call the method {@link AbstractModuleDataDispatcher#fire(AbstractModuleDataDispatcher.ListenerRunnable)}.
     */
    abstract protected class ListenerRunnable implements Runnable {

        private T listener;

        public T getListener() {
            return listener;
        }

        public void setListener(T listener) {
            this.listener = listener;
        }
    }

}
