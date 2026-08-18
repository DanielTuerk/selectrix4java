package io.github.danieltuerk.selectrix4java;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Dispatcher for calls of the registered listeners.
 *
 * @param <T> type of the registered listeners
 * @author Daniel Tuerk
 */
public class AbstractModuleDataDispatcher<T> {

    private final Collection<T> listeners = new ArrayList<>();

    /**
     * Create new dispatcher without registered listeners.
     */
    public AbstractModuleDataDispatcher() {
    }

    /**
     * Register the given listener.
     *
     * @param listener listener to add
     */
    public void addListener(T listener) {
        listeners.add(listener);
    }

    /**
     * Remove the given listener.
     *
     * @param listener listener to remove
     */
    public void removeListener(T listener) {
        listeners.remove(listener);
    }

    /**
     * Remove all registered listeners.
     */
    public void removeAllListeners() {
        listeners.clear();
    }

    /**
     * Registered listeners.
     *
     * @return listeners
     */
    protected Collection<T> getListeners() {
        return listeners;
    }

}
