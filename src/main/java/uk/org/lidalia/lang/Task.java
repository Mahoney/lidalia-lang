package uk.org.lidalia.lang;

import java.util.concurrent.Callable;

import static uk.org.lidalia.lang.Exceptions.throwUnchecked;

/**
 * Adaptation of Runnable to Callable's contract to allow convenient creation of instances of Runnable that throw Exceptions,
 * or instances of Callable&lt;Void&gt; without needing to return null.
 */
public abstract class Task implements Runnable, Callable<Void> {

    /**
     * @return the result of {@link #doRun()}
     * @throws Exception thrown by {@link #doRun()}
     */
    @Override
    public final Void call() throws Exception {
        doRun();
        return null;
    }

    /**
     * @throws Exception thrown by {@link #doRun()} - unchecked
     */
    @Override
    public final void run() {
        try {
            doRun();
        } catch (Exception e) {
            throwUnchecked(e);
        }
    }

    /**
     * Work of this task
     * @throws Exception
     */
    public abstract void doRun() throws Exception;
}
