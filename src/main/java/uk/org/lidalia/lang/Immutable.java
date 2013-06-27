package uk.org.lidalia.lang;

/**
 * Marker interface for classed that are intended to be immutable. In the Java standard library {@link java.lang.String} could 
 * implement this interface.
 * <p>
 * No compile time check is made that the implementing class actually is immutable, this is purely a way of signifying intent.
 */
public interface Immutable<I extends Immutable<I>> extends CanBeMadeImmutable<I> {

    /**
     * @return this instance in all cases
     */
    @Override
    I toImmutable();
}
