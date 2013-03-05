package uk.org.lidalia.lang;

import com.google.common.collect.ImmutableList;

import static uk.org.lidalia.lang.Exceptions.throwableToString;

public class RichException extends Exception {

    private static final long serialVersionUID = 1L;

    private final ImmutableList<Throwable> causes;

    public RichException() {
        super("");
        this.causes = ImmutableList.of();
    }

    public RichException(String message) {
        super(message);
        this.causes = ImmutableList.of();
    }

    public RichException(Throwable cause, Throwable... otherCauses) {
        super(cause);
        this.causes = ImmutableList.<Throwable>builder().add(cause).add(otherCauses).build();
    }

    public RichException(String message, Throwable cause, Throwable... otherCauses) {
        super(message, cause);
        this.causes = ImmutableList.<Throwable>builder().add(cause).add(otherCauses).build();
    }

    public ImmutableList<Throwable> getCauses() {
        return causes;
    }

    @Override
    public String toString() {
        return throwableToString(super.toString(), causes);
    }

    public boolean instanceOf(Class<?> possibleSuperType) {
        return possibleSuperType.isAssignableFrom(getClass());
    }
}
