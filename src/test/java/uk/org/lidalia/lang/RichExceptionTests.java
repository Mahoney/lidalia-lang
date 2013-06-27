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
import static uk.org.lidalia.lang.ShouldThrow.shouldThrow;

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
    public void toStringJustMessage() {
        final RichException exception = new RichException("some message");
        assertThat(exception.toString(), is("uk.org.lidalia.lang.RichException: some message"));
    }

    @Test
    public void toStringMultipleNestedCauses() {
        assertThat(exceptionWithNestedCauses().toString(), is(
                "uk.org.lidalia.lang.RichException: some message"+ lineSeparator()+
                "Caused by: uk.org.lidalia.lang.RichException: cause1 message"+lineSeparator()+
                "Caused by: uk.org.lidalia.lang.RichException: cause1a message"));
    }

    @Test @Ignore
    public void printStackTraceMultipleNestedCauses() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(bytes);

        exceptionWithNestedCauses().printStackTrace(stream);

        assertThat(bytes.toString(), is(
                "uk.org.lidalia.lang.RichException: cause1a message"+lineSeparator()+
                "\tat "+getClass().getName()+".exceptionWithNestedCauses("+getClass().getSimpleName()+".java:104)"));
    }

    private RichException exceptionWithNestedCauses() {
        final RichException cause1a = new RichException("cause1a message");
        final RichException cause1 = new RichException("cause1 message", cause1a);
        return new RichException("some message", cause1);
    }
}
