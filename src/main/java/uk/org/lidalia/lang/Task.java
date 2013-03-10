package uk.org.lidalia.lang;

import java.util.concurrent.Callable;

import static uk.org.lidalia.lang.Exceptions.throwUnchecked;

public abstract class Task implements Runnable, Callable<Void> {

    @Override
    public final Void call() throws Exception {
        doRun();
        return null;
    }

    @Override
    public final void run() {
        try {
            doRun();
        } catch (Exception e) {
            throwUnchecked(e);
        }
    }

    public abstract void doRun() throws Exception;

    public static Task from(final Runnable runnable) {
        return new Task() {
            @Override
            public void doRun() {
                runnable.run();
            }
        };
    }

    public static Task from(final Callable<Void> callable) {
        return new Task() {
            @Override
            public void doRun() throws Exception {
                callable.call();
            }
        };
    }

}
