package uk.org.lidalia.lang;

import java.lang.reflect.Member;

import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import static junitparams.JUnitParamsRunner.$;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static uk.org.lidalia.lang.Modifier.ABSTRACT;
import static uk.org.lidalia.lang.Modifier.FINAL;
import static uk.org.lidalia.lang.Modifier.INTERFACE;
import static uk.org.lidalia.lang.Modifier.NATIVE;
import static uk.org.lidalia.lang.Modifier.PRIVATE;
import static uk.org.lidalia.lang.Modifier.PROTECTED;
import static uk.org.lidalia.lang.Modifier.PUBLIC;
import static uk.org.lidalia.lang.Modifier.STATIC;
import static uk.org.lidalia.lang.Modifier.STRICT;
import static uk.org.lidalia.lang.Modifier.SYNCHRONIZED;
import static uk.org.lidalia.lang.Modifier.TRANSIENT;
import static uk.org.lidalia.lang.Modifier.VOLATILE;

@RunWith(JUnitParamsRunner.class)
public class ModifierTests {

    private final Member memberWithNoModifier;
    private final Class<?> classWithNoModifier;

    public ModifierTests() throws NoSuchMethodException {
        memberWithNoModifier = getMember("fieldWithNoModifier");
        classWithNoModifier = ModifierTests.classWithNoModifier.class;
    }

    public Object[] modifierAndMatchingMember() throws NoSuchMethodException {
        return $(
                $(PUBLIC, getMember("publicField")),
                $(PRIVATE, getMember("privateField")),
                $(PROTECTED, getMember("protectedField")),
                $(STATIC, getMember("staticField")),
                $(FINAL, getMember("finalField")),
                $(SYNCHRONIZED, getMember("synchronizedMethod")),
                $(VOLATILE, getMember("volatileField")),
                $(TRANSIENT, getMember("transientField")),
                $(NATIVE, getMember("nativeMethod")),
                $(STRICT, getMember("strictMethod"))
        );
    }

    @Test
    @Parameters(method = "modifierAndMatchingMember")
    public void modifierExistsOnMember(Modifier modifier, Member member) throws NoSuchMethodException {
        assertThat(modifier.existsOn(member), is(true));
    }

    public Object[] modifierValues() {
        return Modifier.values();
    }

    @Test
    @Parameters(method = "modifierValues")
    public void modifierDoesNotExistOnMemberWithNoModifier(Modifier modifier) throws NoSuchMethodException {
        assertThat(modifier.existsOn(memberWithNoModifier), is(false));
    }

    @Test
    @Parameters(method = "modifierValues")
    public void modifierDoesNotExistOnClassWithNoModifier(Modifier modifier) throws NoSuchMethodException {
        assertThat(modifier.existsOn(classWithNoModifier), is(false));
    }

    /**
     * Suprising result - {@link java.lang.reflect.Modifier#isStrict(int)} returns false when passed
     * {@link strictClass}.class.getModifiers().
     */
    @Test
    public void strictClassDoesNotHaveStrictModifier() {
        assertThat(STRICT.existsOn(strictClass.class), is(false));
    }

    public Object[] modifierAndMatchingClass() throws NoSuchMethodException {
        return $(
                $(PUBLIC, publicClass.class),
                $(PRIVATE, privateClass.class),
                $(PROTECTED, protectedClass.class),
                $(STATIC, staticClass.class),
                $(FINAL, finalClass.class),
                $(INTERFACE, interfaceClass.class),
                $(ABSTRACT, abstractClass.class)
        );
    }

    @Test
    @Parameters(method = "modifierAndMatchingClass")
    public void modifierExistsOnClass(Modifier modifier, Class<?> aClass) {
        assertThat(modifier.existsOn(aClass), is(true));
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
