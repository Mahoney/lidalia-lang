package uk.org.lidalia.lang;

/**
 * Interface for classes that have a mutable representation. In the Java standard library String could implement this interface
 * and return a StringBuilder from toMutable.
 */
public interface CanBeMadeMutable<M> {

    /**
     * @return a mutable representation of this object
     */
    M toMutable();

}
