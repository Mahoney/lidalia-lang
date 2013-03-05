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
     * @return true if and and be are the same type, or a is a subtype of b or vice versa
     */
    public static boolean inSameClassHierarchy(Class<?> a, Class<?> b) {
            return a.isAssignableFrom(b) || b.isAssignableFrom(a);
    }

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
