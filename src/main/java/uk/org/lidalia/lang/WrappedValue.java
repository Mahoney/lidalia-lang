package uk.org.lidalia.lang;

import static com.google.common.base.Preconditions.checkNotNull;
import static uk.org.lidalia.lang.Classes.inSameClassHierarchy;

/**
 * Convenience class for types that represent a higher level abstraction over a single lower level type; for instance
 * a class representing a network Port might extend {@code WrappedValue<Integer>}.
 *
 * Provides implementations of {@link #equals(Object)}, {@link #toString()} and {@link #hashCode()} based on the value
 * of the wrapped instance.
 */
public abstract class WrappedValue {

    private final Object wrapped;

    public WrappedValue(final Object wrapped) {
        this.wrapped = checkNotNull(wrapped);
    }

    /**
     * @return the toString representation of the wrapped instance
     */
    @Override
    public String toString() {
        return wrapped.toString();
    }

    /**
     * Applies equality rules based on the value of the wrapped object.
     * <ul>
     * <li> normal rules in {@link Object#equals(Object)} - not equal to null, equal to same instance
     * <li> other's runtime class must be the same, a super or a sub type of the runtime class of this instance
     * <li> the value of the wrapped instance is the same as the value of the other's wrapped instance
     * </ul>
     * @param other the object to compare against
     * @return true if the other type is logically equal to this
     */
    @Override
    public final boolean equals(final Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (!(other instanceof WrappedValue) || !inSameClassHierarchy(getClass(), other.getClass())) return false;

        final WrappedValue that = (WrappedValue) other;

        return wrapped.equals(that.wrapped);
    }

    /**
     * @return the hashCode of the wrapped instance
     */
    @Override
    public final int hashCode() {
        return wrapped.hashCode();
    }
}
