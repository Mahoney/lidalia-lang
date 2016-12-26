/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package uk.org.lidalia.lang;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater;

abstract class AbstractFuture<V> implements ListenableFuture<V> {
  // NOTE: Whenever both tests are cheap and functional, it's faster to use &, | instead of &&, ||

  private static final boolean GENERATE_CANCELLATION_CAUSES =
      Boolean.parseBoolean(
          System.getProperty("guava.concurrent.generate_cancellation_cause", "false"));

  /**
   * A less abstract subclass of AbstractFuture. This can be used to optimize setFuture by ensuring
   * that {@link #get} calls exactly the implementation of {@link AbstractFuture#get}.
   */
  abstract static class TrustedFuture<V> extends AbstractFuture<V> {
    @Override
    public final V get() throws InterruptedException, ExecutionException {
      return super.get();
    }

    @Override
    public final V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      return super.get(timeout, unit);
    }

    @Override
    public final boolean isDone() {
      return super.isDone();
    }

    @Override
    public final boolean isCancelled() {
      return super.isCancelled();
    }

    @Override
    public final void addListener(Runnable listener, Executor executor) {
      super.addListener(listener, executor);
    }

    @Override
    public final boolean cancel(boolean mayInterruptIfRunning) {
      return super.cancel(mayInterruptIfRunning);
    }
  }

  // Logger to log exceptions caught when running listeners.
  private static final Logger log = Logger.getLogger(AbstractFuture.class.getName());

  // A heuristic for timed gets. If the remaining timeout is less than this, spin instead of
  // blocking. This value is what AbstractQueuedSynchronizer uses.
  private static final long SPIN_THRESHOLD_NANOS = 1000L;

  private static final AtomicHelper ATOMIC_HELPER;

  static {
    AtomicHelper helper;

    try {
      helper = new UnsafeAtomicHelper();
    } catch (Throwable unsafeFailure) {
      // catch absolutely everything and fall through to our 'SafeAtomicHelper'
      // The access control checks that ARFU does means the caller class has to be AbstractFuture
      // instead of SafeAtomicHelper, so we annoyingly define these here
      helper =
          new SafeAtomicHelper(
              newUpdater(Waiter.class, Thread.class, "thread"),
              newUpdater(Waiter.class, Waiter.class, "next"),
              newUpdater(AbstractFuture.class, Waiter.class, "waiters"),
              newUpdater(AbstractFuture.class, Listener.class, "listeners"),
              newUpdater(AbstractFuture.class, Object.class, "value"));

    }
    ATOMIC_HELPER = helper;

    // Prevent rare disastrous classloading in first call to LockSupport.park.
    // See: https://bugs.openjdk.java.net/browse/JDK-8074773
    @SuppressWarnings("unused")
    Class<?> ensureLoaded = LockSupport.class;
  }

  /**
   * Waiter links form a Treiber stack, in the {@link #waiters} field.
   */
  private static final class Waiter {
    static final Waiter TOMBSTONE = new Waiter(false /* ignored param */);

    volatile Thread thread;
    volatile Waiter next;

    /**
     * Constructor for the TOMBSTONE, avoids use of ATOMIC_HELPER in case this class is loaded
     * before the ATOMIC_HELPER. Apparently this is possible on some android platforms.
     */
    Waiter(boolean unused) {}

    Waiter() {
      // avoid volatile write, write is made visible by subsequent CAS on waiters field
      ATOMIC_HELPER.putThread(this, Thread.currentThread());
    }

    // non-volatile write to the next field. Should be made visible by subsequent CAS on waiters
    // field.
    void setNext(Waiter next) {
      ATOMIC_HELPER.putNext(this, next);
    }

    void unpark() {
      // This is racy with removeWaiter. The consequence of the race is that we may spuriously call
      // unpark even though the thread has already removed itself from the list. But even if we did
      // use a CAS, that race would still exist (it would just be ever so slightly smaller).
      Thread w = thread;
      if (w != null) {
        thread = null;
        LockSupport.unpark(w);
      }
    }
  }

  /**
   * Marks the given node as 'deleted' (null waiter) and then scans the list to unlink all deleted
   * nodes. This is an O(n) operation in the common case (and O(n^2) in the worst), but we are saved
   * by two things.
   * <ul>
   * <li>This is only called when a waiting thread times out or is interrupted. Both of which should
   *     be rare.
   * <li>The waiters list should be very short.
   * </ul>
   */
  private void removeWaiter(Waiter node) {
    node.thread = null; // mark as 'deleted'
    restart:
    while (true) {
      Waiter pred = null;
      Waiter curr = waiters;
      if (curr == Waiter.TOMBSTONE) {
        return; // give up if someone is calling complete
      }
      Waiter succ;
      while (curr != null) {
        succ = curr.next;
        if (curr.thread != null) { // we aren't unlinking this node, update pred.
          pred = curr;
        } else if (pred != null) { // We are unlinking this node and it has a predecessor.
          pred.next = succ;
          if (pred.thread == null) { // We raced with another node that unlinked pred. Restart.
            continue restart;
          }
        } else if (!ATOMIC_HELPER.casWaiters(this, curr, succ)) { // We are unlinking head
          continue restart; // We raced with an add or complete
        }
        curr = succ;
      }
      break;
    }
  }

  /** Listeners also form a stack through the {@link #listeners} field. */
  private static final class Listener {
    static final Listener TOMBSTONE = new Listener(null, null);
    final Runnable task;
    final Executor executor;

    // writes to next are made visible by subsequent CAS's on the listeners field
    Listener next;

    Listener(Runnable task, Executor executor) {
      this.task = task;
      this.executor = executor;
    }
  }

  /** A special value to represent {@code null}. */
  private static final Object NULL = new Object();

  /** A special value to represent failure, when {@link #setException} is called successfully. */
  private static final class Failure {
    final Throwable exception;

    Failure(Throwable exception) {
      this.exception = requireNonNull(exception);
    }
  }

  /** A special value to represent cancellation and the 'wasInterrupted' bit. */
  private static final class Cancellation {
    final boolean wasInterrupted;
    final Throwable cause;

    Cancellation(boolean wasInterrupted, Throwable cause) {
      this.wasInterrupted = wasInterrupted;
      this.cause = cause;
    }
  }

  /**
   * This field encodes the current state of the future.
   *
   * <p>The valid values are:
   * <ul>
   * <li>{@code null} initial state, nothing has happened.
   * <li>{@link Cancellation} terminal state, {@code cancel} was called.
   * <li>{@link Failure} terminal state, {@code setException} was called.
   * <li>{@link #NULL} terminal state, {@code set(null)} was called.
   * <li>Any other non-null value, terminal state, {@code set} was called with a non-null argument.
   * </ul>
   */
  private volatile Object value;

  /** All listeners. */
  private volatile Listener listeners;

  /** All waiting threads. */
  private volatile Waiter waiters;

  /**
   * Constructor for use by subclasses.
   */
  private AbstractFuture() {}

  /**
   * {@inheritDoc}
   *
   * <p>The default {@link AbstractFuture} implementation throws {@code InterruptedException} if the
   * current thread is interrupted before or during the call, even if the value is already
   * available.
   *
   * @throws InterruptedException if the current thread was interrupted before or during the call
   *     (optional but recommended).
   * @throws CancellationException {@inheritDoc}
   */
  @Override
  public V get(long timeout, TimeUnit unit)
      throws InterruptedException, TimeoutException, ExecutionException {
    // NOTE: if timeout < 0, remainingNanos will be < 0 and we will fall into the while(true) loop
    // at the bottom and throw a timeoutexception.
    long remainingNanos = unit.toNanos(timeout); // we rely on the implicit null check on unit.
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    Object localValue = value;
    if (localValue != null) {
      return getDoneValue(localValue);
    }
    // we delay calling nanoTime until we know we will need to either park or spin
    final long endNanos = remainingNanos > 0 ? System.nanoTime() + remainingNanos : 0;
    long_wait_loop:
    if (remainingNanos >= SPIN_THRESHOLD_NANOS) {
      Waiter oldHead = waiters;
      if (oldHead != Waiter.TOMBSTONE) {
        Waiter node = new Waiter();
        do {
          node.setNext(oldHead);
          if (ATOMIC_HELPER.casWaiters(this, oldHead, node)) {
            while (true) {
              LockSupport.parkNanos(this, remainingNanos);
              // Check interruption first, if we woke up due to interruption we need to honor that.
              if (Thread.interrupted()) {
                removeWaiter(node);
                throw new InterruptedException();
              }

              // Otherwise re-read and check doneness. If we loop then it must have been a spurious
              // wakeup
              localValue = value;
              if (localValue != null) {
                return getDoneValue(localValue);
              }

              // timed out?
              remainingNanos = endNanos - System.nanoTime();
              if (remainingNanos < SPIN_THRESHOLD_NANOS) {
                // Remove the waiter, one way or another we are done parking this thread.
                removeWaiter(node);
                break long_wait_loop; // jump down to the busy wait loop
              }
            }
          }
          oldHead = waiters; // re-read and loop.
        } while (oldHead != Waiter.TOMBSTONE);
      }
      // re-read value, if we get here then we must have observed a TOMBSTONE while trying to add a
      // waiter.
      return getDoneValue(value);
    }
    // If we get here then we have remainingNanos < SPIN_THRESHOLD_NANOS and there is no node on the
    // waiters list
    while (remainingNanos > 0) {
      localValue = value;
      if (localValue != null) {
        return getDoneValue(localValue);
      }
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }
      remainingNanos = endNanos - System.nanoTime();
    }
    throw new TimeoutException();
  }

  /**
   * {@inheritDoc}
   *
   * <p>The default {@link AbstractFuture} implementation throws {@code InterruptedException} if the
   * current thread is interrupted before or during the call, even if the value is already
   * available.
   *
   * @throws InterruptedException if the current thread was interrupted before or during the call
   *     (optional but recommended).
   * @throws CancellationException {@inheritDoc}
   */
  @Override
  public V get() throws InterruptedException, ExecutionException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    Object localValue = value;
    if (localValue != null) {
      return getDoneValue(localValue);
    }
    Waiter oldHead = waiters;
    if (oldHead != Waiter.TOMBSTONE) {
      Waiter node = new Waiter();
      do {
        node.setNext(oldHead);
        if (ATOMIC_HELPER.casWaiters(this, oldHead, node)) {
          // we are on the stack, now wait for completion.
          while (true) {
            LockSupport.park(this);
            // Check interruption first, if we woke up due to interruption we need to honor that.
            if (Thread.interrupted()) {
              removeWaiter(node);
              throw new InterruptedException();
            }
            // Otherwise re-read and check doneness. If we loop then it must have been a spurious
            // wakeup
            localValue = value;
            if (localValue != null) {
              return getDoneValue(localValue);
            }
          }
        }
        oldHead = waiters; // re-read and loop.
      } while (oldHead != Waiter.TOMBSTONE);
    }
    // re-read value, if we get here then we must have observed a TOMBSTONE while trying to add a
    // waiter.
    return getDoneValue(value);
  }

  private V getDoneValue(Object obj) throws ExecutionException {
    // While this seems like it might be too branch-y, simple benchmarking proves it to be
    // unmeasurable (comparing done AbstractFutures with immediateFuture)
    if (obj instanceof Cancellation) {
      throw cancellationExceptionWithCause(((Cancellation) obj).cause);
    } else if (obj instanceof Failure) {
      throw new ExecutionException(((Failure) obj).exception);
    } else if (obj == NULL) {
      return null;
    } else {
      @SuppressWarnings("unchecked") // this is the only other option
      V asV = (V) obj;
      return asV;
    }
  }

  @Override
  public boolean isDone() {
    final Object localValue = value;
    return localValue != null;
  }

  @Override
  public boolean isCancelled() {
    final Object localValue = value;
    return localValue instanceof Cancellation;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    Object localValue = value;
    boolean rValue = false;
    if (localValue == null) {
      // Try to delay allocating the exception. At this point we may still lose the CAS, but it is
      // certainly less likely.
      Throwable cause =
          GENERATE_CANCELLATION_CAUSES
              ? new CancellationException("Future.cancel() was called.")
              : null;
      Object valueToSet = new Cancellation(mayInterruptIfRunning, cause);
      AbstractFuture<?> abstractFuture = this;
      if (ATOMIC_HELPER.casValue(abstractFuture, null, valueToSet)) {
        rValue = true;
        // We call interuptTask before calling complete(), which is consistent with
        // FutureTask
        complete(abstractFuture);
      }
    }
    return rValue;
  }

  /**
   * Returns true if this future was cancelled with {@code mayInterruptIfRunning} set to {@code
   * true}.
   *
   * @since 14.0
   */
  private boolean wasInterrupted() {
    final Object localValue = value;
    return (localValue instanceof Cancellation) && ((Cancellation) localValue).wasInterrupted;
  }

  /**
   * {@inheritDoc}
   *
   * @since 10.0
   */
  @Override
  public void addListener(Runnable listener, Executor executor) {
    requireNonNull(listener, "Runnable was null.");
    requireNonNull(executor, "Executor was null.");
    Listener oldHead = listeners;
    if (oldHead != Listener.TOMBSTONE) {
      Listener newNode = new Listener(listener, executor);
      do {
        newNode.next = oldHead;
        if (ATOMIC_HELPER.casListeners(this, oldHead, newNode)) {
          return;
        }
        oldHead = listeners; // re-read
      } while (oldHead != Listener.TOMBSTONE);
    }
    // If we get here then the Listener TOMBSTONE was set, which means the future is done, call
    // the listener.
    executeListener(listener, executor);
  }

  boolean set(V value) {
    Object valueToSet = value == null ? NULL : value;
    if (ATOMIC_HELPER.casValue(this, null, valueToSet)) {
      complete(this);
      return true;
    }
    return false;
  }

  boolean setException(Throwable throwable) {
    Object valueToSet = new Failure(requireNonNull(throwable));
    if (ATOMIC_HELPER.casValue(this, null, valueToSet)) {
      complete(this);
      return true;
    }
    return false;
  }

  /** Unblocks all threads and runs all listeners. */
  private static void complete(AbstractFuture<?> future) {

    future.releaseWaiters();
    // We call this before the listeners in order to avoid needing to manage a separate stack data
    // structure for them.
    // afterDone() should be generally fast and only used for cleanup work... but in theory can
    // also be recursive and create StackOverflowErrors
    future.afterDone();
    // push the current set of listeners onto next
    Listener next = future.clearListeners();
    while (next != null) {
      Listener curr = next;
      next = next.next;
      Runnable task = curr.task;

      executeListener(task, curr.executor);
    }

  }

  /**
   * Callback method that is called exactly once after the future is completed.
   *
   * <p>The default implementation of this method in {@code AbstractFuture} does nothing.  This is
   * intended for very lightweight cleanup work, for example, timing statistics or clearing fields.
   * If your task does anything heavier consider, just using a listener with an executor.
   *
   * @since 20.0
   */
  void afterDone() {}

  /**
   * If this future has been cancelled (and possibly interrupted), cancels (and possibly interrupts)
   * the given future (if available).
   *
   * <p>This method should be used only when this future is completed. It is designed to be called
   * from {@code done}.
   */
  final void maybePropagateCancellation(Future<?> related) {
    if (related != null & isCancelled()) {
      related.cancel(wasInterrupted());
    }
  }

  /** Releases all threads in the {@link #waiters} list, and clears the list. */
  private void releaseWaiters() {
    Waiter head;
    do {
      head = waiters;
    } while (!ATOMIC_HELPER.casWaiters(this, head, Waiter.TOMBSTONE));
    for (Waiter currentWaiter = head;
        currentWaiter != null;
        currentWaiter = currentWaiter.next) {
      currentWaiter.unpark();
    }
  }

  /**
   * Clears the {@link #listeners} list and prepends its contents to {@code onto}, least recently
   * added first.
   */
  private Listener clearListeners() {
    // We need to
    // 1. atomically swap the listeners with TOMBSTONE, this is because addListener uses that to
    //    to synchronize with us
    // 2. reverse the linked list, because despite our rather clear contract, people depend on us
    //    executing listeners in the order they were added
    // 3. push all the items onto 'onto' and return the new head of the stack
    Listener head;
    do {
      head = listeners;
    } while (!ATOMIC_HELPER.casListeners(this, head, Listener.TOMBSTONE));
    Listener reversedList = null;
    while (head != null) {
      Listener tmp = head;
      head = head.next;
      tmp.next = reversedList;
      reversedList = tmp;
    }
    return reversedList;
  }

  /**
   * Submits the given runnable to the given {@link Executor} catching and logging all
   * {@linkplain RuntimeException runtime exceptions} thrown by the executor.
   */
  private static void executeListener(Runnable runnable, Executor executor) {
    try {
      executor.execute(runnable);
    } catch (RuntimeException e) {
      // Log it and keep going -- bad runnable and/or executor. Don't punish the other runnables if
      // we're given a bad one. We only catch RuntimeException because we want Errors to propagate
      // up.
      log.log(
          Level.SEVERE,
          "RuntimeException while executing runnable " + runnable + " with executor " + executor,
          e);
    }
  }

  private abstract static class AtomicHelper {
    /** Non volatile write of the thread to the {@link Waiter#thread} field. */
    abstract void putThread(Waiter waiter, Thread newValue);

    /** Non volatile write of the waiter to the {@link Waiter#next} field. */
    abstract void putNext(Waiter waiter, Waiter newValue);

    /** Performs a CAS operation on the {@link #waiters} field. */
    abstract boolean casWaiters(AbstractFuture<?> future, Waiter expect, Waiter update);

    /** Performs a CAS operation on the {@link #listeners} field. */
    abstract boolean casListeners(AbstractFuture<?> future, Listener expect, Listener update);

    /** Performs a CAS operation on the {@link #value} field. */
    abstract boolean casValue(AbstractFuture<?> future, Object expect, Object update);
  }

  /**
   * {@link AtomicHelper} based on {@link sun.misc.Unsafe}.
   *
   * <p>Static initialization of this class will fail if the {@link sun.misc.Unsafe} object cannot
   * be accessed.
   */
  private static final class UnsafeAtomicHelper extends AtomicHelper {
    static final sun.misc.Unsafe UNSAFE;
    static final long LISTENERS_OFFSET;
    static final long WAITERS_OFFSET;
    static final long VALUE_OFFSET;
    static final long WAITER_THREAD_OFFSET;
    static final long WAITER_NEXT_OFFSET;

    static {
      sun.misc.Unsafe unsafe = null;
      try {
        unsafe = sun.misc.Unsafe.getUnsafe();
      } catch (SecurityException tryReflectionInstead) {
        try {
          unsafe =
              AccessController.doPrivileged(
                      (PrivilegedExceptionAction<Unsafe>) () -> {
                        Class<Unsafe> k = Unsafe.class;
                        for (Field f : k.getDeclaredFields()) {
                          f.setAccessible(true);
                          Object x = f.get(null);
                          if (k.isInstance(x)) {
                            return k.cast(x);
                          }
                        }
                        throw new NoSuchFieldError("the Unsafe");
                      });
        } catch (PrivilegedActionException e) {
          throw new RuntimeException("Could not initialize intrinsics", e.getCause());
        }
      }
      try {
        Class<?> abstractFuture = AbstractFuture.class;
        WAITERS_OFFSET = unsafe.objectFieldOffset(abstractFuture.getDeclaredField("waiters"));
        LISTENERS_OFFSET = unsafe.objectFieldOffset(abstractFuture.getDeclaredField("listeners"));
        VALUE_OFFSET = unsafe.objectFieldOffset(abstractFuture.getDeclaredField("value"));
        WAITER_THREAD_OFFSET = unsafe.objectFieldOffset(Waiter.class.getDeclaredField("thread"));
        WAITER_NEXT_OFFSET = unsafe.objectFieldOffset(Waiter.class.getDeclaredField("next"));
        UNSAFE = unsafe;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    void putThread(Waiter waiter, Thread newValue) {
      UNSAFE.putObject(waiter, WAITER_THREAD_OFFSET, newValue);
    }

    @Override
    void putNext(Waiter waiter, Waiter newValue) {
      UNSAFE.putObject(waiter, WAITER_NEXT_OFFSET, newValue);
    }

    /** Performs a CAS operation on the {@link #waiters} field. */
    @Override
    boolean casWaiters(AbstractFuture<?> future, Waiter expect, Waiter update) {
      return UNSAFE.compareAndSwapObject(future, WAITERS_OFFSET, expect, update);
    }

    /** Performs a CAS operation on the {@link #listeners} field. */
    @Override
    boolean casListeners(AbstractFuture<?> future, Listener expect, Listener update) {
      return UNSAFE.compareAndSwapObject(future, LISTENERS_OFFSET, expect, update);
    }

    /** Performs a CAS operation on the {@link #value} field. */
    @Override
    boolean casValue(AbstractFuture<?> future, Object expect, Object update) {
      return UNSAFE.compareAndSwapObject(future, VALUE_OFFSET, expect, update);
    }
  }

  /** {@link AtomicHelper} based on {@link AtomicReferenceFieldUpdater}. */
  private static final class SafeAtomicHelper extends AtomicHelper {
    final AtomicReferenceFieldUpdater<Waiter, Thread> waiterThreadUpdater;
    final AtomicReferenceFieldUpdater<Waiter, Waiter> waiterNextUpdater;
    final AtomicReferenceFieldUpdater<AbstractFuture, Waiter> waitersUpdater;
    final AtomicReferenceFieldUpdater<AbstractFuture, Listener> listenersUpdater;
    final AtomicReferenceFieldUpdater<AbstractFuture, Object> valueUpdater;

    SafeAtomicHelper(
        AtomicReferenceFieldUpdater<Waiter, Thread> waiterThreadUpdater,
        AtomicReferenceFieldUpdater<Waiter, Waiter> waiterNextUpdater,
        AtomicReferenceFieldUpdater<AbstractFuture, Waiter> waitersUpdater,
        AtomicReferenceFieldUpdater<AbstractFuture, Listener> listenersUpdater,
        AtomicReferenceFieldUpdater<AbstractFuture, Object> valueUpdater) {
      this.waiterThreadUpdater = waiterThreadUpdater;
      this.waiterNextUpdater = waiterNextUpdater;
      this.waitersUpdater = waitersUpdater;
      this.listenersUpdater = listenersUpdater;
      this.valueUpdater = valueUpdater;
    }

    @Override
    void putThread(Waiter waiter, Thread newValue) {
      waiterThreadUpdater.lazySet(waiter, newValue);
    }

    @Override
    void putNext(Waiter waiter, Waiter newValue) {
      waiterNextUpdater.lazySet(waiter, newValue);
    }

    @Override
    boolean casWaiters(AbstractFuture<?> future, Waiter expect, Waiter update) {
      return waitersUpdater.compareAndSet(future, expect, update);
    }

    @Override
    boolean casListeners(AbstractFuture<?> future, Listener expect, Listener update) {
      return listenersUpdater.compareAndSet(future, expect, update);
    }

    @Override
    boolean casValue(AbstractFuture<?> future, Object expect, Object update) {
      return valueUpdater.compareAndSet(future, expect, update);
    }
  }

  private static CancellationException cancellationExceptionWithCause(
          Throwable cause) {
    CancellationException exception = new CancellationException("Task was cancelled.");
    exception.initCause(cause);
    return exception;
  }
}
