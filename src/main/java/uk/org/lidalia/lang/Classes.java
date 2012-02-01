package uk.org.lidalia.lang;

import java.lang.reflect.Constructor;

public final class Classes {

	public static boolean instanceOf(Object o, Class<?> c) {
		return c.isAssignableFrom(o.getClass());
	}

	@SuppressWarnings("unchecked")
	public static <CompileTimeType> Class<? extends CompileTimeType> getClass(final CompileTimeType object) {
		return (Class<? extends CompileTimeType>) object.getClass();
	}

	public static boolean inSameClassHierarchy(Class<?> a, Class<?> b) {
			return a.isAssignableFrom(b) || b.isAssignableFrom(a);
	}

	public static boolean hasConstructor(Class<?> type, Class<?>... parameterTypes) throws SecurityException {
		try {
			Constructor<?> constructor = type.getConstructor(parameterTypes);
			return constructor != null;
		} catch (NoSuchMethodException e) {
			return false;
		}

	}

	private Classes() {
		throw new UnsupportedOperationException("Not instantiable");
	}
}
