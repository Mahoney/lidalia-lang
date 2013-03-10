package uk.org.lidalia.lang;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static uk.org.lidalia.lang.Exceptions.throwUnchecked;

/**
 * Wraps and exposes the lazily evaluated result of an expression passed as a Callable to the constructor
 *
 * @param <T> the type of the value wrapped by this class
 */
public class LazyValue<T> implements Callable<T> {

    private final FutureTask<T> supplier;

    /**
     * @param supplier contains the expression (as a Callable) that will create the result; this will be evaluated once
     *                 and only once
     */
    public LazyValue(Callable<T> supplier) {
        this.supplier = new FutureTask<>(supplier);
    }

    /**
     * @throws Exception thrown by the original caller, unwrapped
     * @return the result of the expression passed to the constructor; this is evaluated the first time this method is called,
     *         subsequent calls return the same result without re-evaluating it. Concurrent calls block until the first execution
     *         is complete.
     */
    @Override
    public T call() {
        supplier.run();
        try {
            return supplier.get();
        } catch (Exception e) {
            return throwUnchecked(e.getCause(), null);
        }
    }
}
