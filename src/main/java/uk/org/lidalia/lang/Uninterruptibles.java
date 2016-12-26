/*
 * Copyright (C) 2011 The Guava Authors
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

import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static uk.org.lidalia.lang.Exceptions.throwUnchecked;

/**
 * Copy of method from com.google.common.util.concurrent.Uninterruptibles to break dependency on Guava
 * <p>
 * Utilities for treating interruptible operations as uninterruptible. In all cases, if a thread is
 * interrupted during such a call, the call continues to block until the result is available or the
 * timeout elapses, and only then re-interrupts the thread.
 *
 * @author Anthony Zana
 * @since 10.0
 */
final class Uninterruptibles {

    /**
     * Invokes {@code future.}{@link Future#get() get()} uninterruptibly.
     *
     * @throws ExecutionException    if the computation threw an exception
     * @throws CancellationException if the computation was cancelled
     */
    static <V> V getUninterruptibly(Future<V> future) throws ExecutionException {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    return future.get();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static <V> V getUnchecked(Future<V> future) {
        try {
            return getUninterruptibly(future);
        } catch (ExecutionException e) {
            return throwUnchecked(e.getCause(), null);
        }
    }

    static void sleepUninterruptibly(long sleepFor, TimeUnit unit) {
        boolean interrupted = false;
        try {
            long remainingNanos = unit.toNanos(sleepFor);
            long end = System.nanoTime() + remainingNanos;
            while (true) {
                try {
                    // TimeUnit.sleep() treats negative timeouts just like zero.
                    NANOSECONDS.sleep(remainingNanos);
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                    remainingNanos = end - System.nanoTime();
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private Uninterruptibles() {
        throw new UnsupportedOperationException("Not instantiable");
    }
}
