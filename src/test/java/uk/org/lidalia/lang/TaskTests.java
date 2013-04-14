package uk.org.lidalia.lang;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.org.lidalia.lang.ShouldThrow.shouldThrow;

public class TaskTests {

    @Test public void runCallsDoRun() {
        final AtomicBoolean called = new AtomicBoolean(false);

        new Task() {
            @Override
            public void perform() throws Exception {
                called.set(true);
            }
        }.run();

        assertThat(called.get(), is(true));
    }

    @Test public void runThrowsException() {
        final Exception exception = new Exception();

        final Exception actual = shouldThrow(Exception.class, new Runnable() {
            @Override
            public void run() {
                new Task() {
                    @Override
                    public void perform() throws Exception {
                        throw exception;
                    }
                }.run();
            }
        });
        assertThat(actual, is(exception));
    }

    @Test public void callCallsDoRun() throws Exception {
        final AtomicBoolean called = new AtomicBoolean(false);

        new Task() {
            @Override
            public void perform() throws Exception {
                called.set(true);
            }
        }.call();

        assertThat(called.get(), is(true));
    }

    @Test public void callThrowsException() {
        final Exception exception = new Exception();

        final Exception actual = shouldThrow(Exception.class, new Task() {
            @Override
            public void perform() throws Exception {
                new Task() {
                    @Override
                    public void perform() throws Exception {
                        throw exception;
                    }
                }.call();
            }
        });
        assertThat(actual, is(exception));
    }
}
