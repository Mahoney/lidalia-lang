package uk.org.lidalia.lang;

import static com.google.common.base.Preconditions.checkNotNull;
import static uk.org.lidalia.lang.Classes.inSameClassHierarchy;

public abstract class WrappedValue<E> {

    @Identity private final E wrappedValue;

    public WrappedValue(E wrappedValue) {
        this.wrappedValue = checkNotNull(wrappedValue);
    }

    @Override
    public String toString() {
        return wrappedValue.toString();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!inSameClassHierarchy(getClass(), o.getClass())) return false;

        WrappedValue that = (WrappedValue) o;

        return wrappedValue.equals(that.wrappedValue);
    }

    @Override
    public final int hashCode() {
        return wrappedValue.hashCode();
    }
}
