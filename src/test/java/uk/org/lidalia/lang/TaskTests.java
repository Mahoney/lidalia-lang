package uk.org.lidalia.lang;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.org.lidalia.test.ShouldThrow.shouldThrow;

public class TaskTests {

    @Test public void runCallsDoRun() {
        final AtomicBoolean called = new AtomicBoolean(false);

        new Task() {
            @Override
            public void doRun() throws Exception {
                called.set(true);
            }
        }.run();

        assertThat(called.get(), is(true));
    }

    @Test public void runThrowsException() {
        final Exception exception = new Exception();

        shouldThrow(exception, new Runnable() {
            @Override
            public void run() {
                new Task() {
                    @Override
                    public void doRun() throws Exception {
                        throw exception;
                    }
                }.run();
            }
        });
    }

    @Test public void callCallsDoRun() throws Exception {
        final AtomicBoolean called = new AtomicBoolean(false);

        new Task() {
            @Override
            public void doRun() throws Exception {
                called.set(true);
            }
        }.call();

        assertThat(called.get(), is(true));
    }

    @Test public void callThrowsException() {
        final Exception exception = new Exception();

        shouldThrow(exception, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                return new Task() {
                    @Override
                    public void doRun() throws Exception {
                        throw exception;
                    }
                }.call();
            }
        });
    }
}
