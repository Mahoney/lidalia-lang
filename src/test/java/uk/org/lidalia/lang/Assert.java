package uk.org.lidalia.lang;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.util.Collection;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static uk.org.lidalia.lang.Exceptions.throwUnchecked;

final class Assert {

    static Matcher<Class<?>> isNotInstantiable() {

        final Matcher isAThrowableWhoseMessageIs = is(aThrowableWhoseMessage(is("Not instantiable")));

        final Matcher isAnUnsupportedOperationException = instanceOf(UnsupportedOperationException.class);

        final CombinableMatcher bothIsAnUnsupportedOperationException = CombinableMatcher.both(
                isAThrowableWhoseMessageIs);

        final CombinableMatcher bothIsAnUnsupportedOperationExceptionAndIsAThrowableWhoseMessageIs =
                bothIsAnUnsupportedOperationException
                .and(isAnUnsupportedOperationException);

        final Matcher aConstructorWhoseThrownExceptionBothIsAnUnsupportedOperationExceptionAndIsAThrowableWhoseMessageIs
                = aConstructorWhoseThrownException(bothIsAnUnsupportedOperationExceptionAndIsAThrowableWhoseMessageIs);

        final Matcher aMemberWithModifierPrivate = isAMemberWithModifier(Modifier.PRIVATE);

        final Matcher isACollectionWhoseLengthIs0 = is(Assert.<List<Class<?>>>aCollectionWhoseSize(is(0)));

        final Matcher isAConstructorWhoseParameterTypesAreACollectionWhoseLengthIs0 = is(aConstructorWhoseParameterTypes(isACollectionWhoseLengthIs0));

        final CombinableMatcher<Constructor<?>> isAPrivateNoArgsConstructor = CombinableMatcher.both(
                isAConstructorWhoseParameterTypesAreACollectionWhoseLengthIs0)
                .and(aMemberWithModifierPrivate)
                .and(aConstructorWhoseThrownExceptionBothIsAnUnsupportedOperationExceptionAndIsAThrowableWhoseMessageIs);

        final Matcher<Class<Object>> isEqualToObjectClass = is(equalTo(Object.class));
        final FeatureMatcher<Class<?>, Class<?>> isAClassThatExtendsObjectDirectly = aClassWhoseSuperClass(isEqualToObjectClass);
        final Matcher<List<Constructor<?>>> aSingleElementCollection = Assert.aCollectionWhoseSize(is(1));
        final Matcher<List<Constructor<?>>> isASingleElementCollection = is(aSingleElementCollection);
        final Matcher<List<Constructor<?>>> aListWhoseFirstElementIsAPrivateNoArgsConstructor = Assert.<List<Constructor<?>>, Constructor<?>>aListWhoseElementAtIndex(0, isAPrivateNoArgsConstructor);
        final Matcher<List<Constructor<?>>> isAListWhoseFirstElementIsAPrivateNoArgsConstructor = is(aListWhoseFirstElementIsAPrivateNoArgsConstructor);

        final CombinableMatcher<List<Constructor<?>>> both = CombinableMatcher.both(
                isASingleElementCollection);
        final CombinableMatcher<List<Constructor<?>>> isASinglePrivateNoArgsConstructor = both
                .and(isAListWhoseFirstElementIsAPrivateNoArgsConstructor);
        final FeatureMatcher<Class<?>, List<Constructor<?>>> isAClassWithASinglePrivateNoArgsConstructor = aClassWhoseSetOfConstructors(isASinglePrivateNoArgsConstructor);
        final CombinableMatcher<Class<?>> both1 = CombinableMatcher.both(isAClassThatExtendsObjectDirectly);
        final CombinableMatcher<Class<?>> and = both1.and(isAClassWithASinglePrivateNoArgsConstructor);

        return (CombinableMatcher<Class<?>>) and;
    }

    private static <U, T extends U> FeatureMatcher<Class<? extends T>, Class<? extends U>> aClassWhoseSuperClass(
            final Matcher<? extends Class<? extends U>> classMatcher) {
        return new FeatureMatcher<Class<? extends T>, Class<? extends U>>(
                classMatcher, "a Class whose super class", "'s super class") {
            @Override @SuppressWarnings("unchecked")
            protected Class<? extends U> featureValueOf(final Class<? extends T> actual) {
                return (Class<? extends U>) actual.getSuperclass();
            }
        };
    }

    private static FeatureMatcher<Class<?>, List<Constructor<?>>> aClassWhoseSetOfConstructors(
            final Matcher<List<Constructor<?>>> matcher) {
        return new FeatureMatcher<Class<?>, List<Constructor<?>>>(
                matcher, "a Class whose set of constructors", "'s constructors") {
            @Override
            protected List<Constructor<?>> featureValueOf(final Class<?> actual) {
                return asList(actual.getDeclaredConstructors());
            }
        };
    }

    private static <T extends Collection<?>> Matcher<T> aCollectionWhoseSize(final Matcher<Integer> integerMatcher) {
        return new FeatureMatcher<T, Integer>(integerMatcher, "a Collection whose size", "'s size") {
            @Override
            protected Integer featureValueOf(final T actual) {
                return actual.size();
            }
        };
    }

    private static <T extends List<? extends E>, E> Matcher<T> aListWhoseElementAtIndex(
            final Integer index, final Matcher<E> matcher) {
        return new FeatureMatcher<T, E>(matcher, "a List whose element at index " + index, "'s element at index " + index) {
            @Override
            protected E featureValueOf(final T actual) {
                if (actual.size() > index) {
                    return actual.get(index);
                } else {
                    throw new AssertionError(actual + " has no element at index " + index);
                }
            }
        };
    }

    private static <T extends Member> Matcher<T> isAMemberWithModifier(final Modifier modifier) {
        return new TypeSafeDiagnosingMatcher<T>() {

            @Override
            protected boolean matchesSafely(final T item, final Description mismatchDescription) {
                final boolean matches = modifier.existsOn(item);
                if (!matches) {
                    mismatchDescription.appendValue(item).appendText(" did not have modifier ").appendValue(modifier);
                }
                return matches;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("is a member with modifier " + modifier);
            }
        };
    }

    private static Matcher<Constructor<?>> aConstructorWhoseParameterTypes(final Matcher<List<Class<?>>> parameterMatcher) {
        return new FeatureMatcher<Constructor<?>, List<Class<?>>>(
                parameterMatcher, "a constructor whose parameter types", "'s parameter types") {
            @Override
            protected List<Class<?>> featureValueOf(final Constructor<?> actual) {
                return asList(actual.getParameterTypes());
            }
        };
    }

    private static Matcher<Constructor<?>> aConstructorWhoseThrownException(final Matcher<? extends Throwable> throwableMatcher) {
        return new FeatureMatcher<Constructor<?>, Throwable>(
                throwableMatcher, "a constructor whose thrown exception", "'s thrown exception") {
            @Override
            protected Throwable featureValueOf(final Constructor<?> constructor) {
                try {
                    constructor.setAccessible(true);
                    constructor.newInstance();
                    return null;
                } catch (InvocationTargetException e) {
                    return e.getCause();
                } catch (Exception e) {
                    return throwUnchecked(e, null);
                } finally {
                    constructor.setAccessible(false);
                }
            }
        };
    }

    private static Matcher<Throwable> aThrowableWhoseMessage(final Matcher<String> messageMatcher) {
        return new FeatureMatcher<Throwable, String>(messageMatcher, "a throwable whose message", "'s message") {
            @Override
            protected String featureValueOf(final Throwable actual) {
                return actual.getMessage();
            }
        };
    }

    private Assert() {
        throw new UnsupportedOperationException("Not instantiable");
    }
}
