package uk.org.lidalia.lang;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static uk.org.lidalia.lang.Exceptions.throwUnchecked;

public class LazyValue<T> implements Callable<T> {

    private final FutureTask<T> supplier;

    public LazyValue(Callable<T> supplier) {
        this.supplier = new FutureTask<>(supplier);
    }

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
