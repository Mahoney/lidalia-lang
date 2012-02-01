package uk.org.lidalia.lang;

import java.util.Collections;
import java.util.List;

public class RichRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final List<Throwable> causes;

	public RichRuntimeException() {
		super();
		this.causes = Collections.emptyList();
	}

	public RichRuntimeException(String message) {
		super(message);
		this.causes = Collections.emptyList();
	}

	public RichRuntimeException(Throwable cause, Throwable... otherCauses) {
		super(cause);
		this.causes = Exceptions.buildUnmodifiableCauseList(cause, otherCauses);
	}

	public RichRuntimeException(String message, Throwable cause, Throwable... otherCauses) {
		super(message, cause);
		this.causes = Exceptions.buildUnmodifiableCauseList(cause, otherCauses);
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
