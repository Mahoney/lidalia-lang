package uk.org.lidalia.lang;

import java.util.concurrent.Callable;

import static uk.org.lidalia.lang.Exceptions.throwUnchecked;

public abstract class RunAndCallable implements Runnable, Callable<Void> {

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

    public static RunAndCallable from(final Runnable runnable) {
        return new WrappedRunnable(runnable);
    }

    public static RunAndCallable from(final Callable<Void> callable) {
        return new WrappedCallable(callable);
    }

    private static class WrappedRunnable extends RunAndCallable {
        private final Runnable runnable;

        public WrappedRunnable(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void doRun() {
            runnable.run();
        }
    }

    private static class WrappedCallable extends RunAndCallable {
        private final Callable<Void> callable;

        public WrappedCallable(Callable<Void> callable) {
            this.callable = callable;
        }

        @Override
        public void doRun() throws Exception {
            callable.call();
        }
    }
}
