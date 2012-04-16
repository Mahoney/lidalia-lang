package uk.org.lidalia.lang;

import java.util.concurrent.Callable;

import static uk.org.lidalia.lang.Exceptions.throwUnchecked;

public abstract class RunnableCallable implements Runnable, Callable<Void> {

    @Override
    public final Void call() throws Exception {
        run();
        return null;
    }

    @Override
    public final void run() {
        try {
            run2();
        } catch (Exception e) {
            throwUnchecked(e);
        }
    }

    public abstract void run2() throws Exception;
}
