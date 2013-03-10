package uk.org.lidalia.lang;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class RunAndCallableTests {

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
}
