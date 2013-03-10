package uk.org.lidalia.lang;

import java.lang.reflect.Constructor;

/**
 * A collection of methods that could conveniently exist on {@link Class} but don't.
 */
public final class Classes {

    /**
     * @return the generified compile time type of the object
     */
    @SuppressWarnings("unchecked")
    public static <CompileTimeType> Class<? extends CompileTimeType> getClass(final CompileTimeType object) {
        return (Class<? extends CompileTimeType>) object.getClass();
    }

    /**
     * @return true if a and b are the same type, or a is a subtype of b, or b is a subtype of a
     */
    public static boolean inSameClassHierarchy(Class<?> a, Class<?> b) {
        return a.isAssignableFrom(b) || b.isAssignableFrom(a);
    }

    /**
     * @param type the class to check for the existence of the constructor
     * @param parameterTypes the types of the parameters of the constructor
     * @return true if a constructor with those types exists on the class, false otherwise
     */
    public static boolean hasConstructor(Class<?> type, Class<?>... parameterTypes) {
        try {
            final Constructor<?> constructor = type.getConstructor(parameterTypes);
            return constructor != null;
        } catch (NoSuchMethodException e) {
            return false;
        }

    }

    private Classes() {
        throw new UnsupportedOperationException("Not instantiable");
    }
}
