package uk.org.lidalia.lang;

public abstract class WrappedString extends WrappedValue<String> implements Immutable {

	public WrappedString(String wrappedValue) {
		super(wrappedValue);
	}
	
	@Override
	public final String toString() {
		return wrappedValue;
	}
	
	@Override
	public WrappedString toImmutable() {
		return this;
	}

}
