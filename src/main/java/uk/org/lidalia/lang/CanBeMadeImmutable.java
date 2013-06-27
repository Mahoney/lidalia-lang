package uk.org.lidalia.lang;

/**
 * Interface for classes that have an immutable representation. In the Java standard library {@link StringBuilder} could 
 * implement this interface and return a {@link String} from {@link #toImmutable()}. An immutable class may implement 
 * this interface and return 'this' from {@link #toImmutable()}.
 */
public interface CanBeMadeImmutable<I extends Immutable<I>> {

    /**
     * @return an immutable representation of this object
     */
    I toImmutable();

}
