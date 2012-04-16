package uk.org.lidalia.lang;

public final class WrappedRunnableCallable extends RunnableCallable {

    private final Runnable runnable;

    public WrappedRunnableCallable(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void run2() {
        runnable.run();
    }
}
