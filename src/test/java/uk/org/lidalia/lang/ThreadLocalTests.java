package uk.org.lidalia.lang;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.google.common.base.Supplier;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

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

        assertThat(threadLocal.get(), is("Thread1"));
        assertThat(fromThread.get(), is("Thread2"));
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

        assertThat(threadLocal.get(), is("Initial"));
        assertThat(fromThread.get(), is("Initial"));
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

        assertThat(threadLocal.get(), is("Initial Value"));
        assertThat(fromThread.get(), is("Initial Value"));
    }

    @Test public void initialValueSourceIsCalledSeparatelyPerThread() throws InterruptedException {
        final ThreadLocal<String> threadLocal = new ThreadLocal<String>(new Supplier<String>() {
            @Override
            public String get() {
                return Thread.currentThread().getName();
            }
        });
        final AtomicReference<String> fromThread = new AtomicReference<String>();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                fromThread.set(threadLocal.get());
            }
        });
        thread.start();
        thread.join();

        assertThat(threadLocal.get(), is(Thread.currentThread().getName()));
        assertThat(fromThread.get(), is(thread.getName()));
    }

    @Test public void initialValueSourceIsStateful() throws InterruptedException {
        final ThreadLocal<AtomicReference<String>> threadLocal = new ThreadLocal<AtomicReference<String>>(new Supplier<AtomicReference<String>>() {
            @Override
            public AtomicReference<String> get() {
                return new AtomicReference<String>("initial value");
            }
        });

        threadLocal.get().set("new value");

        assertThat(threadLocal.get().get(), is("new value"));
    }

    @Test public void initialValueSourceIsStatefulOtherThread() throws InterruptedException {
        final ThreadLocal<AtomicReference<String>> threadLocal = new ThreadLocal<AtomicReference<String>>(new Supplier<AtomicReference<String>>() {
            @Override
            public AtomicReference<String> get() {
                return new AtomicReference<String>("initial value");
            }
        });

        final AtomicReference<String> fromThread = new AtomicReference<String>();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                threadLocal.get().set("new value");
                fromThread.set(threadLocal.get().get());
            }
        });
        thread.start();
        thread.join();

        assertThat(fromThread.get(), is("new value"));
    }

    @Test
    public void removeWorks() {
        ThreadLocal<String> threadLocal = new ThreadLocal<String>("Initial Value");
        threadLocal.set("New Value");
        threadLocal.remove();
        assertThat(threadLocal.get(), is("Initial Value"));
    }

    @Test
    public void removeWorksOtherThread() throws InterruptedException {
        final ThreadLocal<String> threadLocal = new ThreadLocal<String>(new Supplier<String>() {
            @Override
            public String get() {
                return Thread.currentThread().getName();
            }
        });
        final AtomicReference<String> fromThread = new AtomicReference<String>();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                threadLocal.set("New Value");
                threadLocal.remove();
                fromThread.set(threadLocal.get());
            }
        });
        thread.start();
        thread.join();

        assertThat(threadLocal.get(), is(Thread.currentThread().getName()));
        assertThat(fromThread.get(), is(thread.getName()));
    }
}
