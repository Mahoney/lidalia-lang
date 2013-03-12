package uk.org.lidalia.lang;

import java.lang.reflect.Field;
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

/**
 * A class that provides implementations of {@link #equals(Object)}, {@link #hashCode()} and {@link #toString()} for its subtypes.
 * <p>
 * These implementations are based on annotating the fields of the subtypes with the {@link Identity} annotation.
 */
public abstract class RichObject {

    private static final int PRIME = 37;
    private static final int INITIAL_HASHCODE_VALUE = 17;

    private static final LoadingCache<Class<?>, FluentIterable<FieldFacade>> IDENTITY_FIELDS =
            CacheBuilder.newBuilder().weakKeys().softValues().build(new IdentityFieldLoader());
    private static final Joiner FIELD_JOINER = Joiner.on(",");
    private static final Function<Object, Integer> toHashCode = new Function<Object, Integer>() {
        @Override
        public Integer apply(final Object fieldValue) {
            return fieldValue.hashCode();
        }
    };

    /**
     * Applies equality rules on the following basis (in addition to the rules in {@link Object#equals(Object)}:
     * <ul>
     * <li> other's runtime class must be the same, a super or a sub type of the runtime class of this instance
     * <li> other's runtime class must have exactly the same set of fields annotated with {@link Identity} as those on the runtime
     *      class of this instance, where the set of fields in each case comprises those on the class and all of its superclasses
     * <li> the value of any field annotated with {@link Identity} on this must be equal to the value of the same field on other
     * </ul>
     * <p>
     * The practical result of this is that an instance of subtype B of subtype A of RichObject can only be equal to an instance
     * of subtype A if B does not annotate any of its fields with {@link Identity}.
     *
     * @param other the object to compare against
     * @return true if the other type is logically equal to this
     */
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

        final RichObject that = (RichObject) other;

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
            public boolean apply(final FieldFacade field) {
                return valueOf(field).equals(other.valueOf(field));
            }
        };
    }

    /**
     * Default implementation of hashCode - can be overridden to provide more efficient ones provided the contract specified
     * in {@link Object#hashCode()} is maintained with respect to {@link #equals(Object)}.
     *
     * @return hash code computed from the hashes of all the fields annotated with {@link Identity}
     */
    @Override public int hashCode() {
        int result = INITIAL_HASHCODE_VALUE;
        for (final FieldFacade field : fields()) {
            final int toAdd = valueOf(field).transform(toHashCode).or(0);
            result = PRIME * result + toAdd;
        }
        return result;
    }

    /**
     * Default implementation of toString.
     *
     * @return a string in the form ClassName[field1=value1,field2=value2] where the fields are those annotated with
     * {@link Identity}
     */
    @Override public String toString() {
        final Iterable<String> fieldsAsStrings = fields().transform(toStringValueOfField());
        return getClass().getSimpleName()+"["+FIELD_JOINER.join(fieldsAsStrings)+"]";
    }

    private Function<FieldFacade, String> toStringValueOfField() {
        return new Function<FieldFacade, String>() {
            @Override
            public String apply(final FieldFacade field) {
                return field.getName() + "=" + valueOf(field).or("absent");
            }
        };
    }

    private Optional<Object> valueOf(final FieldFacade field) {
        return field.valueOn(this);
    }

    private static class IdentityFieldLoader extends CacheLoader<Class<?>, FluentIterable<FieldFacade>> {

        @Override
        public FluentIterable<FieldFacade> load(final Class<?> key) {
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
            public FieldFacade apply(final Field field) {
                return new FieldFacade(field);
            }
        };

        private static final Function<Class<?>, Set<FieldFacade>> toFieldSet = new Function<Class<?>, Set<FieldFacade>>() {
            @Override
            public Set<FieldFacade> apply(final Class<?> input) {
                return doLoad(input);
            }
        };

        private static Set<FieldFacade> doLoad(final Class<?> key) {
            final ImmutableSet<FieldFacade> localIdentityFieldSet = FluentIterable.from(asList(key.getDeclaredFields()))
                    .transform(toFieldFacade)
                    .filter(onlyIdentityFields)
                    .toImmutableSet();
            final Optional<? extends Class<?>> superClass = fromNullable(key.getSuperclass());
            final Set<FieldFacade> superIdentityFieldSet = superClass.transform(toFieldSet).or(ImmutableSet.<FieldFacade>of());
            return Sets.union(localIdentityFieldSet, superIdentityFieldSet);
        }
    }

    private static class FieldFacade extends WrappedValue {
        private final Field field;

        FieldFacade(final Field field) {
            super(field);
            this.field = field;
        }

        public Optional<Object> valueOn(final Object target) {
            try {
                if (!field.isAccessible()) {
                    makeAccessible();
                }
                return fromNullable(field.get(target));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(field+" was not accessible; all fields should be accessible", e);
            }
        }

        public String getName() {
            return field.getName();
        }

        public boolean isIdentityField() {
            return field.isAnnotationPresent(Identity.class);
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
    }
}
