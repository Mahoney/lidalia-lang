package uk.org.lidalia.lang;

import java.lang.reflect.Member;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ModifierTests {

    private final Member memberWithNoModifier;
    private final Class<?> classWithNoModifier;

    public ModifierTests() throws NoSuchMethodException {
        memberWithNoModifier = getMember("fieldWithNoModifier");
        classWithNoModifier = ModifierTests.classWithNoModifier.class;
    }

    @Test
    public void memberTests() throws NoSuchFieldException, NoSuchMethodException {
        memberTest(Modifier.PUBLIC, "publicField");
        memberTest(Modifier.PRIVATE, "privateField");
        memberTest(Modifier.PROTECTED, "protectedField");
        memberTest(Modifier.STATIC, "staticField");
        memberTest(Modifier.FINAL, "finalField");
        memberTest(Modifier.SYNCHRONIZED, "synchronizedMethod");
        memberTest(Modifier.VOLATILE, "volatileField");
        memberTest(Modifier.TRANSIENT, "transientField");
        memberTest(Modifier.NATIVE, "nativeMethod");
        memberTest(Modifier.INTERFACE);
        memberTest(Modifier.ABSTRACT);
        memberTest(Modifier.STRICT, "strictMethod");
    }

    private void memberTest(Modifier modifier, String memberName) throws NoSuchMethodException {
        final Member memberWithModifier = getMember(memberName);
        assertThat(modifier.isTrueOf(memberWithModifier), is(true));
        assertThat(modifier.isTrueOf(memberWithNoModifier), is(false));
    }

    private void memberTest(Modifier modifier) throws NoSuchMethodException {
        assertThat(modifier.isTrueOf(memberWithNoModifier), is(false));
    }

    @Test
    public void classTests() {
        classTest(Modifier.PUBLIC, publicClass.class);
        classTest(Modifier.PRIVATE, privateClass.class);
        classTest(Modifier.PROTECTED, protectedClass.class);
        classTest(Modifier.STATIC, staticClass.class);
        classTest(Modifier.FINAL, finalClass.class);
        classTest(Modifier.SYNCHRONIZED);
        classTest(Modifier.VOLATILE);
        classTest(Modifier.TRANSIENT);
        classTest(Modifier.NATIVE);
        classTest(Modifier.INTERFACE, interfaceClass.class);
        classTest(Modifier.ABSTRACT, abstractClass.class);
    }

    @Test
    public void strictClassDoesNotHaveStrictModifier() {
        assertThat(Modifier.STRICT.isTrueOf(classWithNoModifier), is(false));
        assertThat(Modifier.STRICT.isTrueOf(strictClass.class), is(false));
    }

    private void classTest(Modifier modifier, Class<?> aClass) {
        assertThat(modifier.isTrueOf(aClass), is(true));
        assertThat(modifier.isTrueOf(classWithNoModifier), is(false));
    }

    private void classTest(Modifier modifier) {
        assertThat(modifier.isTrueOf(classWithNoModifier), is(false));
    }

    private Member getMember(String memberName) throws NoSuchMethodException {
        try {
            return ModifierTests.class.getDeclaredField(memberName);
        } catch (NoSuchFieldException exception) {
            return ModifierTests.class.getDeclaredMethod(memberName);
        }
    }

    public byte publicField;
    protected byte protectedField;
    private byte privateField;
    final byte finalField = 0;
    static byte staticField;
    synchronized void synchronizedMethod() {}
    volatile byte volatileField;
    transient byte transientField;
    native void nativeMethod();
    strictfp void strictMethod() {}
    byte fieldWithNoModifier;

    public class publicClass {}
    protected class protectedClass {}
    private class privateClass {}
    final class finalClass {}
    static class staticClass {}
    strictfp class strictClass {}
    abstract class abstractClass {}
    interface interfaceClass {}
    class classWithNoModifier {}
}
