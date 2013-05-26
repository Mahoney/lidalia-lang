package uk.org.lidalia.lang;

import org.junit.Test;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import static org.junit.Assert.assertNull;

public class ThreadLocalClassLoaderTests {

    @Test
    public void classLoaderCanBeGarbageCollectedAfterSettingAValueOnAThreadLocal() throws Exception {
        ClassLoader classLoader = SimpleClassloader.make();
        WeakReference<ClassLoader> referenceToClassLoader =
                new WeakReference<>(classLoader, new ReferenceQueue<ClassLoader>());

        Runnable threadLocalUser = (Runnable) classLoader.loadClass(ThreadLocalUser.class.getName()).newInstance();
        threadLocalUser.run();

        threadLocalUser = null;
        classLoader = null;

        System.gc();

        assertNull("classLoader has not been garbage collected", referenceToClassLoader.get());
    }
}
