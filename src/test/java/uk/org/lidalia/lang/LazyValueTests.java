package uk.org.lidalia.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.org.lidalia.test.ShouldThrow.shouldThrow;

public class LazyValueTests {

    private final Callable<String> supplier = mock(Callable.class);

    @Test
    public void returnsExpectedValue() throws Exception {
        given(supplier.call()).willReturn("expected value");
        final LazyValue<String> lazyValue = new LazyValue<>(supplier);
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
        given(supplier.call()).willThrow(expectedException);
        shouldThrow(expectedException, new Runnable() {
            @Override
            public void run() {
                new LazyValue<>(supplier).call();
            }
        });
    }

    @Test
    public void threadSafeAndEvaluatedOnlyOnce() throws Exception {
        given(supplier.call()).willReturn("expected value");

        final LazyValue<String> lazyValue = new LazyValue<>(supplier);

        final CountDownLatch start = new CountDownLatch(1);
        int runs = 100;
        final ExecutorService executor = Executors.newFixedThreadPool(runs);
        final List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < runs; i++) {
            results.add(executor.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    start.await();
                    return lazyValue.call();
                }
            }));
        }

        start.countDown();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
        assertThat(Iterables.all(results, new Predicate<Future<String>>() {
            @Override
            public boolean apply(Future<String> input) {
                try {
                    return input.get().equals("expected value");
                } catch (Exception e) {
                    return false;
                }
            }
        }), is(true));
        verify(supplier, times(1)).call();
    }
}
