package uk.org.lidalia.lang;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static uk.org.lidalia.lang.Exceptions.throwUnchecked;

public class AsyncValue<T> implements Callable<T> {

    private final Future<T> supplier;

    public AsyncValue(Callable<T> supplier) {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        this.supplier = executor.submit(supplier);
        executor.shutdown();
    }

    @Override
    public T call() {
        try {
            return supplier.get();
        } catch (Exception e) {
            return throwUnchecked(e.getCause(), null);
        }
    }
}
