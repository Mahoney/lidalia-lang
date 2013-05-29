package uk.org.lidalia.lang;

import org.junit.Test;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ThreadLocalClassLoaderTests {

    @Test
    public void classLoaderCanBeGarbageCollectedAfterSettingAValueOnAThreadLocal() throws Exception {
        WeakReference<ClassLoader> referenceToClassLoader = useClassInClassLoaderAndTryToGarbageCollect(LidaliaThreadLocalUser.class);

        assertNull("classLoader has not been garbage collected", referenceToClassLoader.get());
    }

    @Test
    public void classLoaderCannotBeGarbageCollectedAfterSettingAValueOnAJavaLangThreadLocal() throws Exception {
        WeakReference<ClassLoader> referenceToClassLoader = useClassInClassLoaderAndTryToGarbageCollect(JavaLangThreadLocalUser.class);

        assertNotNull("classLoader has been garbage collected", referenceToClassLoader.get());
    }

    private WeakReference<ClassLoader> useClassInClassLoaderAndTryToGarbageCollect(Class<? extends Runnable> threadLocalUserClass) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        ClassLoader classLoader = SimpleClassloader.make();
        WeakReference<ClassLoader> referenceToClassLoader =
                new WeakReference<>(classLoader, new ReferenceQueue<ClassLoader>());

        Runnable threadLocalUser = (Runnable) classLoader.loadClass(threadLocalUserClass.getName()).newInstance();
        threadLocalUser.run();

        threadLocalUser = null;
        classLoader = null;

        System.gc();
        return referenceToClassLoader;
    }

    public static class JavaLangThreadLocalUser implements Runnable {

        private final java.lang.ThreadLocal<Modifier> threadLocal = new java.lang.ThreadLocal<>();

        @Override
        public void run() {
            threadLocal.set(Modifier.FINAL);
        }
    }

    public static class LidaliaThreadLocalUser implements Runnable {

        private final ThreadLocal<Modifier> threadLocal = new ThreadLocal<>(Modifier.ABSTRACT);

        @Override
        public void run() {
            threadLocal.set(Modifier.FINAL);
        }
    }
}
