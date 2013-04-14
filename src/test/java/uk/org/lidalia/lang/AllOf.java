package uk.org.lidalia.lang;

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;

/**
 * Calculates the logical conjunction of multiple matchers. Evaluation is shortcut, so
 * subsequent matchers are not called if an earlier matcher returns <code>false</code>.
 */
class AllOf<T> extends DiagnosingMatcher<T> {

    private final Iterable<Matcher<? super T>> matchers;

    AllOf(final Iterable<Matcher<? super T>> matchers) {
        super();
        this.matchers = matchers;
    }

    @Override
    public boolean matches(final Object object, final Description mismatch) {
        for (final Matcher<? super T> matcher : matchers) {
            if (!matcher.matches(object)) {
                matcher.describeMismatch(object, mismatch);
                return false;
            }
        }
        return true;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendList("(", " " + "and" + " ", ")", matchers);
    }
}
