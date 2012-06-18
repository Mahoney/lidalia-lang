package uk.org.lidalia.lang;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A ThreadLocal that has no ClassLoader leaks associated with it.
 * @param <T>
 */
public class SafeThreadLocal<T> {

    private final Map<Thread, T> contents = new ConcurrentHashMap<Thread, T>();
    private final Supplier<T> initialValueCreator;

    public SafeThreadLocal(final T initialValue) {
        this(new Supplier<T>() {
            @Override
            public T get() {
                return checkNotNull(initialValue);
            }
        });
    }

    public SafeThreadLocal(final Supplier<T> initialValueCreator) {
        checkNotNull(initialValueCreator);
        this.initialValueCreator = initialValueCreator;
        set(initialValueCreator.get());
    }

    public void set(T value) {
        contents.put(Thread.currentThread(), checkNotNull(value));
    }

    public T get() {
        T value = contents.get(Thread.currentThread());
        if (value == null) {
            T initialValue = initialValueCreator.get();
            set(initialValue);
            return initialValue;
        }
        return value;
    }

    public void remove() {
        contents.remove(Thread.currentThread());
    }

    public void reset() {
        contents.clear();
    }
}
