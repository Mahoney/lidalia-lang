package uk.org.lidalia.lang;

import static com.google.common.base.Optional.fromNullable;

public class RichRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RichRuntimeException() {
        super("");
    }

    public RichRuntimeException(String message) {
        super(message);
    }

    public RichRuntimeException(Throwable cause) {
        super(cause);
    }

    public RichRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String toString() {
        return Exceptions.throwableToString(super.toString(), fromNullable(getCause()));
    }

    public boolean instanceOf(Class<?> possibleSuperType) {
        return possibleSuperType.isAssignableFrom(getClass());
    }
}
