package uk.org.lidalia.lang;

import java.util.concurrent.Callable;

import static uk.org.lidalia.lang.Exceptions.throwUnchecked;

/**
 * Adaptation of Runnable to Callable's contract to allow convenient creation of instances of Runnable that throw Exceptions,
 * or instances of Callable&lt;Void&gt; without needing to return null.
 */
public abstract class Task implements Runnable, Callable<Void> {

    /**
     * @return the result of {@link #perform()}
     * @throws Exception thrown by {@link #perform()}
     */
    @Override
    public final Void call() throws Exception { //NOPMD no way of knowing what Exception implementer will throw
        perform();
        return null;
    }

    /**
     * @throws Exception thrown by {@link #perform()} - unchecked
     */
    @Override
    public final void run() {
        try {
            perform();
        } catch (Exception e) {
            throwUnchecked(e);
        }
    }

    /**
     * Work of this task.
     * @throws Exception
     */
    public abstract void perform() throws Exception; //NOPMD no way of knowing what Exception implementer will throw
}
