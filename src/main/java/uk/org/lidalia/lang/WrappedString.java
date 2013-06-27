package uk.org.lidalia.lang;

public abstract class WrappedString<T extends WrappedString<T>> extends WrappedValue implements Immutable<T> {

    public WrappedString(String wrappedValue) {
        super(wrappedValue);
    }

    @Override
    public T toImmutable() {
        return (T) this;
    }
}
