package uk.org.lidalia.lang;

import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static com.google.common.collect.ImmutableList.of;
import static java.lang.System.lineSeparator;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static uk.org.lidalia.test.ShouldThrow.shouldThrow;

public class RichExceptionTests {

    @Test
    public void messageEmpty() {
        final RichException exception = new RichException();
        assertThat(exception.getMessage(), is(""));
    }

    @Test
    public void messageHasValue() {
        final RichException exception = new RichException("some message");
        assertThat(exception.getMessage(), is("some message"));
    }

    @Test
    public void noCause() {
        final RichException exception = new RichException();
        assertThat(exception.getCause(), is(nullValue()));
    }

    @Test
    public void oneCause() {
        final Throwable cause = new Exception();

        final RichException exception = new RichException(cause);

        assertThat(exception.getCause(), is(cause));
        assertThat(exception.getCauses(), is(of(cause)));
    }

    @Test
    public void multipleCauses() {
        final Throwable cause1 = new Exception();
        final Throwable cause2 = new Exception();
        final Throwable cause3 = new Exception();

        final RichException exception = new RichException(cause1, cause2, cause3);

        assertThat(exception.getCause(), is(cause1));
        assertThat(exception.getCauses(), is(of(cause1, cause2, cause3)));
    }

    @Test
    public void nullCause() {
        shouldThrow(NullPointerException.class, new Runnable() {
            @Override
            public void run() {
                new RichException("", null);
            }
        });
    }

    @Test
    public void nullOtherCause() {
        shouldThrow(NullPointerException.class, new Runnable() {
            @Override
            public void run() {
                new RichException("", new Exception(), null);
            }
        });
    }

    @Test
    public void toStringJustMessage() {
        final RichException exception = new RichException("some message");
        assertThat(exception.toString(), is("uk.org.lidalia.lang.RichException: some message"));
    }

    @Test
    public void toStringMultipleNestedCauses() {
        assertThat(exceptionWithNestedCauses().toString(), is(
                "uk.org.lidalia.lang.RichException: some message"+ lineSeparator()+
                "caused by: uk.org.lidalia.lang.RichException: cause1 message"+lineSeparator()+
                " caused by: uk.org.lidalia.lang.RichException: cause1a message"+lineSeparator()+
                "caused by: uk.org.lidalia.lang.RichException: cause2 message"+lineSeparator()+
                " caused by: uk.org.lidalia.lang.RichException: cause2a message"));
    }

    @Test @Ignore
    public void printStackTraceMultipleNestedCauses() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(bytes);

        exceptionWithNestedCauses().printStackTrace(stream);

        assertThat(bytes.toString(), is(
                "uk.org.lidalia.lang.RichException: cause1a message"+lineSeparator()+
                "\tat uk.org.lidalia.lang.RichExceptionTests.exceptionWithNestedCauses(RichExceptionTests.java:104)"));
    }

    private RichException exceptionWithNestedCauses() {
        final RichException cause1a = new RichException("cause1a message");
        final RichException cause1 = new RichException("cause1 message", cause1a);
        final RichException cause2a = new RichException("cause2a message");
        final RichException cause2 = new RichException("cause2 message", cause2a);
        return new RichException("some message", cause1, cause2);
    }
}
