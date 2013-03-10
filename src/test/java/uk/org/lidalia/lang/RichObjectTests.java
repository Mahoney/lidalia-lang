package uk.org.lidalia.lang;

import static com.google.common.base.Optional.fromNullable;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;

import com.google.common.base.Function;

import uk.org.lidalia.lang.testclasses.ClassA;
import uk.org.lidalia.lang.testclasses.ClassA1;
import uk.org.lidalia.lang.testclasses.ClassA2;
import uk.org.lidalia.lang.testclasses.ClassB;
import uk.org.lidalia.lang.testclasses.NoFields;

public class RichObjectTests {

    @Test public void equalsObject() {
        RichObject richObject = new RichObject();
        Object object = new Object();
        assertThat(richObject, isNotEqualTo(object));
    }

    @Test public void equalsOnInstanceOfSameClass() {
        ClassA o1 = new ClassA();
        o1.setValue1("hello");
        assertThat(o1, isEqualTo(o1));
        assertThat(o1, isNotEqualTo(null));

        ClassA o2 = new ClassA();
        assertThat(o1, isNotEqualTo(o2));
        assertThat(o2, isNotEqualTo(o1));

        o2.setValue1("hello");
        assertThat(o1, isEqualTo(o2));
        assertThat(o2, isEqualTo(o1));
    }

    @Test public void equalsOnInstanceOfSameClassMultipleIdentityMembers() {
        ClassA2 o1 = new ClassA2();
        o1.setValue1("hello");

        ClassA2 o2 = new ClassA2();
        o2.setValue1("hello");

        o1.setNewIdentityValue("world");
        assertThat(o1, isNotEqualTo(o2));
        assertThat(o2, isNotEqualTo(o1));

        o2.setNewIdentityValue("world");
        assertThat(o1, isEqualTo(o2));
        assertThat(o2, isEqualTo(o1));
    }

    @Test public void equalsOnInstanceOfClassesNotAssignableFromEachOther() {
        ClassA o1 = new ClassA();
        o1.setValue1("hello");
        ClassB o2 = new ClassB();
        o2.setValue1("hello");

        assertThat(o1, isNotEqualTo(o2));
        assertThat(o2, isNotEqualTo(o1));
    }

    @Test public void equalsOnInstancesOfClassesAssignableFromEachOtherWithSameIdentityMembers() {
        ClassA o1 = new ClassA();
        o1.setValue1("hello");
        ClassA1 o2 = new ClassA1();
        o2.setValue1("hello");
        o2.setNonIdentityValue("world");
        
        assertThat(o2, isEqualTo(o1));
        assertThat(o1, isEqualTo(o2));
    }

    @Test public void equalsOnInstancesOfClassesAssignableFromEachOtherWithDifferentIdentityMembers() {
        ClassA o1 = new ClassA();
        o1.setValue1("hello");
        ClassA2 o2 = new ClassA2();
        o2.setValue1("hello");
        o2.setNewIdentityValue("world");
        
        assertThat(o1, isNotEqualTo(o2));
        assertThat(o2, isNotEqualTo(o1));
    }

    @Test public void toStringWorks() {
        ClassA2 o = new ClassA2();
        o.setValue1("hello");
        o.setNewIdentityValue("world");
        assertThat(o.toString(), is("ClassA2[newIdentityValue=world,value1=hello]"));
    }

    @Test public void equalsNoFields() {
        assertThat(new NoFields(), isEqualTo(new NoFields()));
    }

    @Test public void toStringNoFields() {
        assertThat(new NoFields().toString(), is("NoFields[]"));
    }

    @Test public void differentIdentityFieldsHaveDifferentHashCodes() {
        ClassA o1 = new ClassA();
        o1.setValue1("hello");

        ClassA o2 = new ClassA();
        o1.setValue1("world");

        assertThat(o1.hashCode(), isNotEqualTo(o2.hashCode()));
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
