package uk.org.lidalia.lang;

import java.util.concurrent.Callable;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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
}
