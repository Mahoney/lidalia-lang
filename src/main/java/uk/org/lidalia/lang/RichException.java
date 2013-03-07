package uk.org.lidalia.lang;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkNotNull;
import static uk.org.lidalia.lang.Exceptions.throwableToString;

public class RichException extends Exception {

    private static final long serialVersionUID = 1L;

    public RichException() {
        super("");
    }

    public RichException(String message) {
        super(message);
    }

    public RichException(Throwable cause) {
        super(checkNotNull(cause));
    }

    public RichException(String message, Throwable cause) {
        super(message, checkNotNull(cause));
    }

    @Override
    public String toString() {
        return throwableToString(super.toString(), fromNullable(getCause()));
    }

    public boolean instanceOf(Class<?> possibleSuperType) {
        return possibleSuperType.isAssignableFrom(getClass());
    }
}
