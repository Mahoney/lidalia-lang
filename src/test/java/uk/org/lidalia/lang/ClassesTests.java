package uk.org.lidalia.lang;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import uk.org.lidalia.lang.testclasses.ClassA;
import uk.org.lidalia.lang.testclasses.ClassA1;
import uk.org.lidalia.lang.testclasses.ClassC1;
import uk.org.lidalia.lang.testclasses.ClassC2;
import uk.org.lidalia.lang.testclasses.InterfaceC;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.org.lidalia.lang.Classes.hasConstructor;
import static uk.org.lidalia.lang.Classes.inSameClassHierarchy;
import static uk.org.lidalia.lang.Assert.isNotInstantiable;

public class ClassesTests {

    @Test
    public void getClassGenerified() throws IllegalAccessException, InstantiationException {
        final List<String> aList = new ArrayList<String>();
        final Class<? extends List<String>> aClass = Classes.getClass(aList);
        final List<String> strings = aClass.newInstance();
        assertThat(strings, is(equalTo(aList)));
    }

    @Test
    public void inSameClassHierarchyWhenimplementingSameInterfaceOtherwiseUnrelated() {
        assertThat(inSameClassHierarchy(ClassC1.class, InterfaceC.class), is(true));
        assertThat(inSameClassHierarchy(ClassC2.class, InterfaceC.class), is(true));
        assertThat(inSameClassHierarchy(ClassC1.class, ClassC2.class), is(false));
    }

    @Test
    public void inSameClassHierarchyReflexive() {
        assertThat(inSameClassHierarchy(ClassA.class, ClassA.class), is(true));
    }

    @Test
    public void inSameClassHierarchySymmetric() {
        assertThat(inSameClassHierarchy(ClassA.class, ClassA1.class), is(true));
        assertThat(inSameClassHierarchy(ClassA1.class, ClassA.class), is(true));
    }

    @Test
    public void notInstantiable() {
        assertThat(Classes.class, isNotInstantiable());
    }

    @Test
    public void hasConstructorTrue() {
        assertThat(hasConstructor(String.class, byte[].class), is(true));
    }

    @Test
    public void hasConstructorFalse() {
        assertThat(hasConstructor(String.class, Object.class), is(false));
    }
}
