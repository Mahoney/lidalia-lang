package uk.org.lidalia.lang;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.PrivilegedAction;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import static com.google.common.base.Optional.fromNullable;
import static java.security.AccessController.doPrivileged;
import static java.util.Arrays.asList;
import static uk.org.lidalia.lang.Classes.inSameClassHierarchy;
import static uk.org.lidalia.lang.Exceptions.throwUnchecked;

public class RichObject {

    private static final int PRIME = 37;
    private static final int INITIAL_HASHCODE_VALUE = 17;

    private static final LoadingCache<Class<?>, FluentIterable<FieldFacade>> IDENTITY_FIELDS =
            CacheBuilder.newBuilder().weakKeys().softValues().build(new IdentityFieldLoader());
    private static final Joiner FIELD_JOINER = Joiner.on(",");
    private static final Function<Object,Integer> toHashCode = new Function<Object, Integer>() {
        @Override
        public Integer apply(Object fieldValue) {
            return fieldValue.hashCode();
        }
    };

    @Override public final boolean equals(final Object other) {
        // Usual equals checks
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
        }

        // One of the two must be a subtype of the other
        if (!(other instanceof RichObject) || !inSameClassHierarchy(getClass(), other.getClass())) {
            return false;
        }

        RichObject that = (RichObject) other;

        // They must have precisely the same set of identity members to meet the
        // symmetric & transitive requirement of equals
        final FluentIterable<FieldFacade> fieldsOfThis = fields();
        return fieldsOfThis.toImmutableSet().equals(that.fields().toImmutableSet())
                && fieldsOfThis.allMatch(hasEqualValueIn(that));
    }

    private FluentIterable<FieldFacade> fields() {
        try {
            return IDENTITY_FIELDS.get(getClass());
        } catch (ExecutionException e) {
            return throwUnchecked(e.getCause(), null);
        }
    }

    private Predicate<FieldFacade> hasEqualValueIn(final RichObject other) {
        return new Predicate<FieldFacade>() {
            @Override
            public boolean apply(FieldFacade field) {
                return RichObject.this.valueOf(field).equals(other.valueOf(field));
            }
        };
    }

    @Override public int hashCode() {
        int result = INITIAL_HASHCODE_VALUE;
        for (final FieldFacade field : fields()) {
            final int toAdd = this.valueOf(field).transform(toHashCode).or(0);
            result = PRIME * result + toAdd;
        }
        return result;
    }

    @Override public String toString() {
        final Iterable<String> fieldsAsStrings = fields().transform(toStringValueOfField());
        return ""+getClass().getSimpleName()+"["+FIELD_JOINER.join(fieldsAsStrings)+"]";
    }

    private Function<FieldFacade, String> toStringValueOfField() {
        return new Function<FieldFacade, String>() {
            @Override
            public String apply(FieldFacade field) {
                return field.getName() + "=" + valueOf(field).or("absent");
            }
        };
    }

    private Optional<Object> valueOf(FieldFacade field) {
        return field.valueOn(this);
    }

    private static class IdentityFieldLoader extends CacheLoader<Class<?>, FluentIterable<FieldFacade>> {

        @Override
        public FluentIterable<FieldFacade> load(Class<?> key) {
            return FluentIterable.from(doLoad(key));
        }

        private static final Predicate<FieldFacade> onlyIdentityFields = new Predicate<FieldFacade>() {
            @Override
            public boolean apply(final FieldFacade field) {
                return field.isIdentityField();
            }
        };

        private static final Function<Field, FieldFacade> toFieldFacade = new Function<Field, FieldFacade>() {
            @Override
            public FieldFacade apply(Field field) {
                return new FieldFacade(field);
            }
        };

        private static final Function<Class<?>,Set<FieldFacade>> toFieldSet = new Function<Class<?>, Set<FieldFacade>>() {
            @Override
            public Set<FieldFacade> apply(Class<?> input) {
                return doLoad(input);
            }
        };

        private static Set<FieldFacade> doLoad(Class<?> key) {
            final ImmutableSet<FieldFacade> localIdentityFieldSet = FluentIterable.from(asList(key.getDeclaredFields()))
                    .transform(toFieldFacade)
                    .filter(onlyIdentityFields)
                    .toImmutableSet();
            final Optional<? extends Class<?>> superClass = fromNullable(key.getSuperclass());
            final Set<FieldFacade> superIdentityFieldSet = superClass.transform(toFieldSet).or(ImmutableSet.<FieldFacade>of());
            return Sets.union(localIdentityFieldSet, superIdentityFieldSet);
        }
    }

    private static class FieldFacade extends WrappedValue<Field> {
        private final Field field;

        private FieldFacade(Field field) {
            super(field);
            this.field = field;
        }

        public Optional<Object> valueOn(Object target) {
            try {
                return fromNullable(field.get(target));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(field+" was not accessible; all fields should be accessible", e);
            }
        }

        public String getName() {
            return field.getName();
        }

        public boolean isIdentityField() {
            final boolean isIdentityField = field.isAnnotationPresent(Identity.class);
            if (isIdentityField && !isPublic()) {
                makeAccessible();
            }
            return isIdentityField;
        }

        private void makeAccessible() {
            doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    field.setAccessible(true);
                    return null;
                }
            });
        }

        public boolean isPublic() {
            return Modifier.isPublic(field.getModifiers());
        }
    }
}
