package uk.org.lidalia.lang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as forming part of the identity of a class that extends {@link RichObject}. {@link RichObject#equals(Object)},
 * {@link RichObject#hashCode()} and {@link RichObject#toString()} are all driven from fields marked with this annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface Identity {

}
