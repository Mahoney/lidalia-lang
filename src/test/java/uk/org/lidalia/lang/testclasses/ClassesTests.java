package uk.org.lidalia.lang.testclasses;

import org.junit.Test;

import uk.org.lidalia.lang.Classes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassesTests {

    @Test
    public void inSameClassHierarchyWhenimplementingSameInterfaceOtherwiseUnrelated() {
        assertThat(Classes.inSameClassHierarchy(ClassC1.class, ClassC2.class), is(false));
    }
}
