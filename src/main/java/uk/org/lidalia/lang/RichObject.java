package uk.org.lidalia.lang;

import static com.google.common.base.Objects.equal;
import static uk.org.lidalia.lang.Classes.inSameClassHierarchy;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Function;

public class RichObject {
	private static final Function<Class<?>, Set<Field>> GET_IDENTITY_FIELDS = CachedFunction.make(new Function<Class<?>, Set<Field>>() {
		@Override
		public Set<Field> apply(Class<?> clazz) {
			Set<Field> result = new HashSet<Field>();
			for (Field field : clazz.getDeclaredFields()) {
				if (field.isAnnotationPresent(Identity.class)) {
					if (!Modifier.isPublic(field.getModifiers())) {
						field.setAccessible(true);
					}
					result.add(field);
				}
			}
			Class<?> superclass = clazz.getSuperclass();
			if (superclass != RichObject.class) {
				result.addAll(GET_IDENTITY_FIELDS.apply(superclass));
			}
			return Collections.unmodifiableSet(result);
		}
		
	});
	
	@Override public final boolean equals(Object other) {
		// Usual equals checks
		if (other == this) {
			return true;
		}
		if (other == null) {
			return false;
		}

		// One of the two must be a subtype of the other
		if (!inSameClassHierarchy(this.getClass(), other.getClass())) {
			return false;
		}

		// They must have precisely the same set of identity members to meet the
		// symmetric & transitive requirement of equals
		Set<Field> thisFields = GET_IDENTITY_FIELDS.apply(this.getClass());
		if (thisFields.size() == 0) {
			return false;
		}
		Set<Field> otherFields = GET_IDENTITY_FIELDS.apply(other.getClass());
		if (!thisFields.equals(otherFields)) {
			return false;
		}
		
		try {
			for (Field field : thisFields) {
				if (!equal(field.get(other), field.get(this)))
					return false;
			}
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("All fields should be accessible", e);
		}
		return true;
	}

    private volatile int hashCode = -1;

    @Override public int hashCode() {

		if (hashCode != -1) {
			return hashCode;
		}

		final int prime = 37;
		int result = 17;
		try {
			Set<Field> thisFields = GET_IDENTITY_FIELDS.apply(this.getClass());
			for (Field field : thisFields) {
				Object value = field.get(this);
				result = prime * result + (value == null ? 0 : value.hashCode());
			}
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("All fields should be accessible", e);
		}
		if (this instanceof Immutable) {
			hashCode = result;
		}
		return result;
	}

    private volatile String toString = null;

	@Override public String toString() {

		if (toString != null) {
			return toString;
		}

		Set<Field> thisFields = GET_IDENTITY_FIELDS.apply(this.getClass());
		if (thisFields.isEmpty()) {
			return super.toString();
		}
		
		StringBuilder builder = new StringBuilder(this.getClass().getSimpleName());
		builder.append(Collections3.toString(thisFields, "[", ", ", "]", new Function<Field, String>() {
			@Override public String apply(Field field) {
				try {
					return field.getName() + "=" + field.get(RichObject.this);
				} catch (IllegalAccessException e) {
					throw new IllegalStateException("All fields should be accessible", e);
				}
			}
		}));

		if (this instanceof Immutable) {
			toString = builder.toString();
		}
		return builder.toString();
	}

    public boolean instanceOf(Class<?> possibleSuperType) {
        return Classes.instanceOf(this, possibleSuperType);
    }
}
