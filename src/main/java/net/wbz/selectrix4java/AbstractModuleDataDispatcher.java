package net.wbz.selectrix4java;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Dispatcher for async calls of the registered listeners.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class AbstractModuleDataDispatcher<T> {

    private final Queue<T> listeners = new ConcurrentLinkedQueue<>();
    private final ExecutorService executorService;

    public AbstractModuleDataDispatcher() {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat(
                this.getClass().getSimpleName() + "-data-dispatcher-%d").build();
        executorService = Executors.newCachedThreadPool(namedThreadFactory);
    }

    public void addListener(T listener) {
        listeners.add(listener);
    }

    public void removeListener(T listener) {
        listeners.remove(listener);
    }

    /**
     * TODO: multi usage of one runnable possible?
     *
     * Execute the given runnable for all registered listeners.
     *
     * @param runnable {@link AbstractModuleDataDispatcher.ListenerRunnable}
     */
    protected void fire(ListenerRunnable runnable) {
        for (T listener : listeners) {
            runnable.setListener(listener);
            executorService.submit(runnable);
        }
    }

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
