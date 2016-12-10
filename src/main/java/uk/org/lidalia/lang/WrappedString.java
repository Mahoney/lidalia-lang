package uk.org.lidalia.lang;

public abstract class WrappedString<T extends WrappedString<T>> extends WrappedValue {

    public WrappedString(String wrappedValue) {
        super(wrappedValue);
    }

}
