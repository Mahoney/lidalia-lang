package uk.org.lidalia.lang;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class WrappedValue<E> extends RichObject {

    @Identity private final E wrappedValue;

    public WrappedValue(E wrappedValue) {
        this.wrappedValue = checkNotNull(wrappedValue);
    }

    @Override
    public String toString() {
        return wrappedValue.toString();
    }

    protected E getWrappedValue() {
        return wrappedValue;
    }
}
