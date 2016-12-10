package uk.org.lidalia.lang;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static java.security.AccessController.doPrivileged;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.empty;
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

    private static final LoadingCache<Class<?>, List<FieldFacade>> IDENTITY_FIELDS =
            CacheBuilder.newBuilder().weakKeys().softValues().build(new IdentityFieldLoader());

    /**
     * Implementation of equals based on fields annotated with {@link Identity}.
     *
     * Applies equality rules on the following basis (in addition to the rules in {@link Object#equals(Object)}):
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
        final List<FieldFacade> fieldsOfThis = fields();
        return fieldsOfThis.equals(that.fields())
                && fieldsOfThis.stream().allMatch(field -> valueOf(field).equals(that.valueOf(field)));
    }

    private List<FieldFacade> fields() {
        try {
            return IDENTITY_FIELDS.get(getClass());
        } catch (ExecutionException e) {
            return throwUnchecked(e.getCause(), null);
        }
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
            final int toAdd = valueOf(field).map(Object::hashCode).orElse(0);
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
        final Stream<String> fieldsAsStrings = fields().stream().map(field -> field.getName() + "=" + valueOf(field).orElse("null"));
        return getClass().getSimpleName()+"["+fieldsAsStrings.collect(joining(","))+"]";
    }

    private Optional<Object> valueOf(final FieldFacade field) {
        return field.valueOn(this);
    }

    private static class IdentityFieldLoader extends CacheLoader<Class<?>, List<FieldFacade>> {

        @Override
        public List<FieldFacade> load(final Class<?> key) {
            return doLoad(key).collect(toList());
        }

        private static Stream<FieldFacade> doLoad(final Class<?> key) {
            final Stream<FieldFacade> localIdentityFieldSet = stream(key.getDeclaredFields()).map(FieldFacade::new)
                    .filter(FieldFacade::isIdentityField);
            final Optional<Class<?>> superClass = ofNullable(key.getSuperclass());
            final Stream<FieldFacade> superIdentityFieldSet = superClass.map(IdentityFieldLoader::doLoad).orElse(empty());
            return Stream.concat(localIdentityFieldSet, superIdentityFieldSet);
        }
    }

    private static class FieldFacade extends WrappedValue {

        private final Field field;

        FieldFacade(final Field field) {
            super(field);
            this.field = field;
        }

        Optional<Object> valueOn(final Object target) {
            try {
                if (!field.isAccessible()) {
                    makeAccessible();
                }
                return ofNullable(field.get(target));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(field+" was not accessible; all fields should be accessible", e);
            }
        }

        String getName() {
            return field.getName();
        }

        boolean isIdentityField() {
            return field.isAnnotationPresent(Identity.class);
        }

        private void makeAccessible() {
            doPrivileged((PrivilegedAction<Void>) () -> {
                field.setAccessible(true);
                return null;
            });
        }
    }
}
