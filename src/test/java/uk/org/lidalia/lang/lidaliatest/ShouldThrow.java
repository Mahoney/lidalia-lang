package uk.org.lidalia.lang.lidaliatest;

import com.google.common.base.Optional;

import uk.org.lidalia.lang.Classes;

import static org.junit.Assert.assertSame;
import static uk.org.lidalia.lang.Exceptions.throwUnchecked;

public final class ShouldThrow {

    public static <ThrowableType extends Throwable> ThrowableType shouldThrow(
            final Class<ThrowableType> expectedThrowableType,
            final Runnable workThatShouldThrowThrowable) {
        return shouldThrow(expectedThrowableType, Optional.<String>absent(), workThatShouldThrowThrowable);
    }

    public static <ThrowableType extends Throwable> ThrowableType shouldThrow(
            final Class<ThrowableType> expectedThrowableType,
            final String message,
            final Runnable workThatShouldThrowThrowable) {
        return shouldThrow(expectedThrowableType, Optional.of(message), workThatShouldThrowThrowable);
    }

    public static <ThrowableType extends Throwable> void shouldThrow(
            final ThrowableType expectedThrowable,
            final Runnable workThatShouldThrowThrowable) {
        shouldThrow(expectedThrowable, Optional.<String>absent(), workThatShouldThrowThrowable);
    }

    public static <ThrowableType extends Throwable> void shouldThrow(
            final ThrowableType expectedThrowable,
            final String message,
            final Runnable workThatShouldThrowThrowable) {
        shouldThrow(expectedThrowable, Optional.of(message), workThatShouldThrowThrowable);
    }

    private static <ThrowableType extends Throwable> void shouldThrow(
            final ThrowableType expectedThrowable,
            final Optional<String> message,
            final Runnable workThatShouldThrowThrowable) {
        final ThrowableType actualThrowable = shouldThrow(Classes.getClass(expectedThrowable), message, workThatShouldThrowThrowable);
        assertSame(message.or("Did not throw correct Throwable;"), expectedThrowable, actualThrowable);
    }

    @SuppressWarnings("unchecked")
    private static <ThrowableType extends Throwable> ThrowableType shouldThrow(
            final Class<ThrowableType> expectedThrowableType,
            final Optional<String> message,
            final Runnable workThatShouldThrowThrowable) {
        try {
            workThatShouldThrowThrowable.run();
            throw new AssertionError(message.or("No exception thrown"));
        } catch (final Throwable actualThrowableThrown) { // NOPMD Throwable is thrown if it was not expected
            if (!expectedThrowableType.isInstance(actualThrowableThrown)) {
                throwUnchecked(actualThrowableThrown);
            }
            return (ThrowableType) actualThrowableThrown;
        }
    }

    private ShouldThrow() {
        throw new UnsupportedOperationException("Not instantiable");
    }
}
