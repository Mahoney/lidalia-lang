package uk.org.lidalia.lang;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;

import com.google.common.base.Function;

import static com.google.common.base.Optional.fromNullable;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class WrappedValueTests {

    @Test public void equalToOtherObject() {
        assertThat(new WrappedString("hi"), isNotEqualTo(new Object()));
    }

    @Test public void equalToSelf() {
        final WrappedString instance = new WrappedString("hi");
        assertThat(instance, isEqualTo(instance));
    }

    @Test public void equalToOtherInstanceSameValue() {
        assertThat(new WrappedString("hi"), isEqualTo(new WrappedString("hi")));
    }

    @Test public void equalToOtherInstanceDifferentValue() {
        assertThat(new WrappedString("hi"), isNotEqualTo(new WrappedString("there")));
    }

    @Test public void equalToNull() {
        assertThat(new WrappedString("hi"), isNotEqualTo(null));
    }

    @Test public void equalToSubtype() {
        assertThat(new WrappedNumber(1), isEqualTo(new SubWrappedNumber(1)));
        assertThat(new SubWrappedNumber(1), isEqualTo(new WrappedNumber(1)));
        assertThat(new WrappedNumber(1), isNotEqualTo(new SubWrappedNumber(2)));
        assertThat(new SubWrappedNumber(2), isNotEqualTo(new WrappedNumber(1)));
    }

    @Test public void equalToOtherWrappedValueDifferentHierarchy() {
        assertThat(new WrappedNumber(1), isNotEqualTo(new WrappedInteger(1)));
        assertThat(new WrappedInteger(1), isNotEqualTo(new WrappedNumber(1)));
    }

    @Test public void toStringValue() {
        assertThat(new WrappedInteger(3).toString(), is("3"));
        assertThat(new WrappedString("hi").toString(), is("hi"));
    }

    @Test public void hashCodeValue() {
        assertThat(new WrappedInteger(3).hashCode(), is(Integer.valueOf(3).hashCode()));
        assertThat(new WrappedString("hello").hashCode(), is("hello".hashCode()));
    }

    private static class WrappedNumber extends WrappedValue {
        public WrappedNumber(Number wrappedValue) {
            super(wrappedValue);
        }
    }

    private static class SubWrappedNumber extends WrappedNumber {
        public SubWrappedNumber(Integer wrappedValue) {
            super(wrappedValue);
        }
    }

    private static class WrappedInteger extends WrappedValue {
        public WrappedInteger(Integer wrappedValue) {
            super(wrappedValue);
        }
    }

    private static class WrappedString extends WrappedValue {
        public WrappedString(String wrappedValue) {
            super(wrappedValue);
        }
    }

    private Matcher<Object> isEqualTo(Object other) {
        return both(is(equalTo(other))).and(hashCode(is(equalTo(hashCodeOf(other)))));
    }

    private Matcher<Object> isNotEqualTo(Object other) {
        return is(not(equalTo(other)));
    }

    private FeatureMatcher<Object, Integer> hashCode(final Matcher<Integer> notEqualToHashCode) {
        return new FeatureMatcher<Object, Integer>(notEqualToHashCode, "hashCode", "hashCode") {
            @Override
            protected Integer featureValueOf(Object actual) {
                return hashCodeOf(actual);
            }
        };
    }

    private Integer hashCodeOf(Object actual) {
        return fromNullable(actual).transform(new Function<Object, Integer>() {
            @Override
            public Integer apply(Object input) {
                return input.hashCode();
            }
        }).orNull();
    }
}
