package uk.org.lidalia.lang;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.google.common.base.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class ThreadLocalTests {

    @Test public void differentValuePerThread() throws InterruptedException {
        final ThreadLocal<String> threadLocal = new ThreadLocal<String>("Initial");
        final AtomicReference<String> fromThread = new AtomicReference<String>();
        threadLocal.set("Thread1");

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                threadLocal.set("Thread2");
                fromThread.set(threadLocal.get());
            }
        });
        thread.start();
        thread.join();

        assertEquals("Thread1", threadLocal.get());
        assertEquals("Thread2", fromThread.get());
    }

    @Test public void resetWorksForAllThreads() throws InterruptedException {
        final ThreadLocal<String> threadLocal = new ThreadLocal<String>("Initial");
        final AtomicReference<String> fromThread = new AtomicReference<String>();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                threadLocal.set("Thread2");
                threadLocal.reset();
                fromThread.set(threadLocal.get());
            }
        });
        thread.start();
        thread.join();

        assertEquals("Initial", threadLocal.get());
        assertEquals("Initial", threadLocal.get());
    }

    @Test public void initialValueWorksForAllThreads() throws InterruptedException {
        final ThreadLocal<String> threadLocal = new ThreadLocal<String>("Initial Value");
        final AtomicReference<String> fromThread = new AtomicReference<String>();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                fromThread.set(threadLocal.get());
            }
        });
        thread.start();
        thread.join();

        assertEquals("Initial Value", threadLocal.get());
        assertEquals("Initial Value", fromThread.get());
    }

    @Test public void initialValueSourceIsCalledSeparatelyPerThread() throws InterruptedException {
        final ThreadLocal<Object> threadLocal = new ThreadLocal<Object>(new Supplier<Object>() {
            @Override
            public Object get() {
                return new Object();
            }
        });
        final AtomicReference<Object> fromThread = new AtomicReference<Object>();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                fromThread.set(threadLocal.get());
            }
        });
        thread.start();
        thread.join();

        assertNotSame(threadLocal.get(), fromThread.get());
    }
}
