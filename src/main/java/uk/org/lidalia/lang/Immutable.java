package uk.org.lidalia.lang;

public interface Immutable extends CanBeMadeImmutable {
	// marker interface.  any implementing class can return 'this' for toImmutable()
}
