package uk.org.lidalia.lang.lidaliatest;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

class CombinableMatcher<T> extends TypeSafeDiagnosingMatcher<T> {
    private final Matcher<? super T> matcher;

    CombinableMatcher(final Matcher<? super T> matcher) {
        super();
        this.matcher = matcher;
    }

    @Override
    protected boolean matchesSafely(final T item, final Description mismatch) {
        if (!matcher.matches(item)) {
            matcher.describeMismatch(item, mismatch);
            return false;
        }
        return true;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendDescriptionOf(matcher);
    }

    CombinableMatcher<T> and(final Matcher<? super T> other) {
        return new CombinableMatcher<>(new AllOf<>(templatedListWith(other)));
    }

    private List<Matcher<? super T>> templatedListWith(final Matcher<? super T> other) {
        final List<Matcher<? super T>> matchers = new ArrayList<>();
        matchers.add(matcher);
        matchers.add(other);
        return matchers;
    }

    /**
     * Creates a matcher that matches when both of the specified matchers match the examined object.
     * <p/>
     * For example:
     * <pre>assertThat("fab", both(containsString("a")).and(containsString("b")))</pre>
     */
    @Factory
    static <LHS> CombinableMatcher<LHS> both(final Matcher<? super LHS> matcher) {
        return new CombinableMatcher<>(matcher);
    }
}
