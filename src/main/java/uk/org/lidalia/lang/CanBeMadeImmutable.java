package uk.org.lidalia.lang;

/**
 * Interface for classes that have an immutable representation. In the Java standard library java.lang.StringBuilder could implement
 * this interface and return a java.lang.String from toImmutable. An immutable class may implement this interface and return
 * this from toImmutable.
 */
public interface CanBeMadeImmutable<I extends Immutable<I>> {

    /**
     * @return an immutable representation of this object
     */
    I toImmutable();

}
