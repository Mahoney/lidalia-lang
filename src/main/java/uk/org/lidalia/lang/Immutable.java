package uk.org.lidalia.lang;

/**
 * Marker interface for classed that are intended to be immutable.
 * Any implementing class should return 'this' for toImmutable().
 * No compile time check is made that the implementing class actually is immutable, this is purely a way of signifying intent.
 */
public interface Immutable<I extends Immutable<I>> extends CanBeMadeImmutable<I> {
}
