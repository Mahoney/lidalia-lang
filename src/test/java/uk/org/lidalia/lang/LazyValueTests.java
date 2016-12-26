package uk.org.lidalia.lang;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.org.lidalia.lang.ShouldThrow.shouldThrow;
import static uk.org.lidalia.lang.Uninterruptibles.sleepUninterruptibly;

public class LazyValueTests {

    @SuppressWarnings("unchecked")
    private final Callable<String> supplier = mock(Callable.class);

    @Test
    public void returnsExpectedValue() throws Exception {

        final LazyValue<String> lazyValue = new LazyValue<>(() -> "expected value");

        assertThat(lazyValue.call(), is("expected value"));
    }

    @Test
    public void supplierNotEvaluatedOnInstantiation() {
        new LazyValue<>(supplier);
        verifyZeroInteractions(supplier);
    }

    @Test
    public void throwsSourceException() throws Exception {

        final Exception expectedException = new Exception();

        final Exception actual = shouldThrow(Exception.class, () ->
                new LazyValue<>(() -> {
                    throw expectedException;
                }).call()
        );

        assertThat(actual, is(expectedException));
    }

    @Test
    public void handlesInterruption() throws Exception {
        final LazyValue<String> lazyValue = new LazyValue<>(() -> {
            sleepUninterruptibly(200, MILLISECONDS);
            return "result";
        });
        final AtomicReference<String> result = new AtomicReference<>();
        final AtomicReference<Boolean> interrupted = new AtomicReference<>(false);
        Thread t = new Thread(() -> {
                result.set(lazyValue.call());
                interrupted.set(Thread.currentThread().isInterrupted());
        }, "test-thread");
        t.start();
        t.interrupt();
        t.join();
        assertThat(result.get(), is("result"));
        assertThat(interrupted.get(), is(true));
    }

    @Test
    public void threadSafeAndEvaluatedOnlyOnce() throws Exception {

        given(supplier.call()).willReturn("expected value");

        final List<String> results = concurrently(100, new LazyValue<>(supplier));

        assertThat(results, everyItem(is("expected value")));

        verify(supplier, times(1)).call();
    }

    @Test
    public void toStringWhenNotYetEvaluated() throws Exception {
        given(supplier.call()).willReturn("a value");
        final LazyValue<String> lazyValue = new LazyValue<>(supplier);

        assertThat(lazyValue.toString(), is("not yet evaluated"));
    }

    @Test
    public void toStringWhenEvaluated() throws Exception {
        given(supplier.call()).willReturn("expected value");
        final LazyValue<String> lazyValue = new LazyValue<>(supplier);

        lazyValue.call();

        assertThat(lazyValue.toString(), is("expected value"));
    }

    private static <T> List<T> concurrently(int runs, Callable<T> work) throws InterruptedException {
        final CountDownLatch start = new CountDownLatch(1);
        final ExecutorService executor = Executors.newFixedThreadPool(runs);
        final List<Future<T>> results = new ArrayList<>();


        for (int i = 0; i < runs; i++) {

            results.add(executor.submit(() ->{
                start.await();
                return work.call();
            }));
        }

        start.countDown();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        return results.stream().map(Uninterruptibles::getUnchecked).collect(Collectors.toList());
    }
}
