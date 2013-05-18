package uk.org.lidalia.lang;

import static uk.org.lidalia.lang.Exceptions.throwUnchecked;

final class ShouldThrow {

    @SuppressWarnings({ "unchecked", "PMD.AvoidCatchingThrowable" })
    static <ThrowableType extends Throwable> ThrowableType shouldThrow(
            final Class<ThrowableType> expectedThrowableType,
            final Runnable workThatShouldThrowThrowable) {
        try {
            workThatShouldThrowThrowable.run();
            throw new AssertionError("No exception thrown");
        } catch (final Throwable actualThrowableThrown) {
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
