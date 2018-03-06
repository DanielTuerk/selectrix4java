package net.wbz.selectrix4java;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Dispatcher for calls of the registered listeners.
 *
 * @author Daniel Tuerk
 */
public class AbstractModuleDataDispatcher<T> {

    private final Collection<T> listeners = new ArrayList<>();

    public AbstractModuleDataDispatcher() {
    }

    public void addListener(T listener) {
        listeners.add(listener);
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

}
