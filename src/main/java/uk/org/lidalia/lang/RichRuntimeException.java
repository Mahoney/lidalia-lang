package uk.org.lidalia.lang;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class RichRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ImmutableList<Throwable> causes;

    public RichRuntimeException() {
        super();
        this.causes = ImmutableList.of();
    }

    public RichRuntimeException(String message) {
        super(message);
        this.causes = ImmutableList.of();
    }

    public RichRuntimeException(Throwable cause, Throwable... otherCauses) {
        super(cause);
        this.causes = ImmutableList.<Throwable>builder().add(cause).add(otherCauses).build();
    }

    public RichRuntimeException(String message, Throwable cause, Throwable... otherCauses) {
        super(message, cause);
        this.causes = ImmutableList.<Throwable>builder().add(cause).add(otherCauses).build();
    }

    public List<Throwable> getCauses() {
        return causes;
    }

    @Override
    public String toString() {
        return Exceptions.throwableToString(super.toString(), causes);
    }

    public boolean instanceOf(Class<?> possibleSuperType) {
        return possibleSuperType.isAssignableFrom(getClass());
    }
}
