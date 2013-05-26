package uk.org.lidalia.lang;

public class ThreadLocalUser implements Runnable {

    private final ThreadLocal<Modifier> threadLocal = new ThreadLocal<>(Modifier.ABSTRACT);

    @Override
    public void run() {
        threadLocal.set(Modifier.FINAL);
    }
}
