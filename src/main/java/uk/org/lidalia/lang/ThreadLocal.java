package uk.org.lidalia.lang;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Supplier;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Thread.currentThread;

/**
 * A ThreadLocal that has no {@link ClassLoader} leaks associated with it & does not permit null.
 *
 * Values for all {@link Thread}s can be reset from any {@link Thread}.
 *
 * @param <T> the type of the thread local variable
 */
public class ThreadLocal<T> {

    private final Map<Thread, T> contents = new ConcurrentHashMap<>();
    private final Supplier<T> initialValueCreator;
    private final Supplier<T> threadValueInitialiser = new Supplier<T>() {
        @Override
        public T get() {
            final T initialValue = initialValueCreator.get();
            set(initialValue);
            return initialValue;
        }
    };

    /**
     * @param initialValue the value this thread local will initially have for all {@link Thread}s.
     *                     This should not be mutable, as it will be shared between all {@link Thread}s.
     */
    public ThreadLocal(final T initialValue) {
        this(new Supplier<T>() {
            @Override
            public T get() {
                return initialValue;
            }
        });
    }

    /**
     * @param initialValueCreator a {@link Supplier} whose get method is called on a per {@link Thread} basis
     *                            in order to establish the initial value for that {@link Thread},
     *                            allowing a different initial instance per {@link Thread}.
     */
    public ThreadLocal(final Supplier<T> initialValueCreator) {
        this.initialValueCreator = checkNotNull(initialValueCreator);
    }

    /**
     * @param value the new value for the calling {@link Thread} - does not affect the value for any other {@link Thread}.
     */
    public void set(final T value) {
        contents.put(currentThread(), checkNotNull(value));
    }

    /**
     * @return the value for the calling {@link Thread}, or the initial value if this has not been set or has been removed.
     */
    public T get() {
        return fromNullable(contents.get(currentThread())).or(threadValueInitialiser);
    }

    /**
     * Removes the value for the calling {@link Thread}.
     * A subsequent call to {@link #get()} will return the initial value.
     */
    public void remove() {
        contents.remove(currentThread());
    }

    /**
     * Removes the values for ALL {@link Thread}s.
     * Subsequent calls to {@link #get()} will return the initial value.
     */
    public void reset() {
        contents.clear();
    }
}
