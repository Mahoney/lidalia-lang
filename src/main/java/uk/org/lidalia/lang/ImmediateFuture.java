/*
 * Copyright (C) 2006 The Guava Authors
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

import uk.org.lidalia.lang.AbstractFuture.TrustedFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * Implementations of {@code Futures.immediate*}.
 */
abstract class ImmediateFuture<V> implements ListenableFuture<V> {
  private static final Logger log = Logger.getLogger(ImmediateFuture.class.getName());

  @Override
  public void addListener(Runnable listener, Executor executor) {
    requireNonNull(listener, "Runnable was null.");
    requireNonNull(executor, "Executor was null.");
    try {
      executor.execute(listener);
    } catch (RuntimeException e) {
      // ListenableFuture's contract is that it will not throw unchecked exceptions, so log the bad
      // runnable and/or executor and swallow it.
      log.log(
          Level.SEVERE,
          "RuntimeException while executing runnable " + listener + " with executor " + executor,
          e);
    }
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  @Override
  public abstract V get() throws ExecutionException;

  @Override
  public V get(long timeout, TimeUnit unit) throws ExecutionException {
    requireNonNull(unit);
    return get();
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return true;
  }

  static class ImmediateSuccessfulFuture<V> extends ImmediateFuture<V> {
    static final ImmediateFuture.ImmediateSuccessfulFuture<Object> NULL =
            new ImmediateFuture.ImmediateSuccessfulFuture<>(null);
    private final V value;

    ImmediateSuccessfulFuture(V value) {
      this.value = value;
    }

    // TODO(lukes): Consider throwing InterruptedException when appropriate.
    @Override
    public V get() {
      return value;
    }
  }

  static final class ImmediateFailedFuture<V> extends TrustedFuture<V> {
    ImmediateFailedFuture(Throwable thrown) {
      setException(thrown);
    }
  }

}
