package uk.org.lidalia.lang.testclasses;

import org.junit.Test;

import uk.org.lidalia.lang.Classes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.org.lidalia.lang.Classes.inSameClassHierarchy;
import static uk.org.lidalia.test.Assert.isNotInstantiable;

public class ClassesTests {

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
}
