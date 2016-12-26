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

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static uk.org.lidalia.lang.Futures.getDone;
import static uk.org.lidalia.lang.MoreExecutors.directExecutor;

/**
 * Implementations of {@code Futures.transform*}.
 */
abstract class AbstractTransformFuture<I, O, F, T> extends AbstractFuture.TrustedFuture<O>
    implements Runnable {


  static <I, O> ListenableFuture<O> create(
      ListenableFuture<I> input, Function<? super I, ? extends O> function) {
    requireNonNull(function);
    TransformFuture<I, O> output = new TransformFuture<>(input, function);
    input.addListener(output, directExecutor());
    return output;
  }

  /*
   * In certain circumstances, this field might theoretically not be visible to an afterDone() call
   * triggered by cancel(). For details, see the comments on the fields of TimeoutFuture.
   */
  private ListenableFuture<? extends I> inputFuture;
  private F function;

  AbstractTransformFuture(ListenableFuture<? extends I> inputFuture, F function) {
    this.inputFuture = requireNonNull(inputFuture);
    this.function = requireNonNull(function);
  }

  @Override
  public final void run() {
    ListenableFuture<? extends I> localInputFuture = inputFuture;
    F localFunction = function;
    if (isCancelled() | localInputFuture == null | localFunction == null) {
      return;
    }
    inputFuture = null;
    function = null;

    /*
     * Any of the setException() calls below can fail if the output Future is cancelled between now
     * and then. This means that we're silently swallowing an exception -- maybe even an Error. But
     * this is no worse than what FutureTask does in that situation. Additionally, because the
     * Future was cancelled, its listeners have been run, so its consumers will not hang.
     *
     * Contrast this to the situation we have if setResult() throws, a situation described below.
     */

    I sourceResult;
    try {
      sourceResult = getDone(localInputFuture);
    } catch (CancellationException e) {
      // Cancel this future and return.
      // At this point, inputFuture is cancelled and outputFuture doesn't exist, so the value of
      // mayInterruptIfRunning is irrelevant.
      cancel(false);
      return;
    } catch (ExecutionException e) {
      // Set the cause of the exception as this future's exception.
      setException(e.getCause());
      return;
    } catch (RuntimeException | Error e) {
      // Bug in inputFuture.get(). Propagate to the output Future so that its consumers don't hang.
      setException(e);
      return;
    }

    T transformResult;
    try {
      transformResult = doTransform(localFunction, sourceResult);
    } catch (UndeclaredThrowableException e) {
      // Set the cause of the exception as this future's exception.
      setException(e.getCause());
      return;
    } catch (Throwable t) {
      // This exception is irrelevant in this thread, but useful for the client.
      setException(t);
      return;
    }

    /*
     * If set()/setValue() throws an Error, we let it propagate. Why? The most likely Error is a
     * StackOverflowError (from deep transform(..., directExecutor()) nesting), and calling
     * setException(stackOverflowError) would fail:
     *
     * - If the stack overflowed before set()/setValue() could even store the result in the output
     * Future, then a call setException() would likely also overflow.
     *
     * - If the stack overflowed after set()/setValue() stored its result, then a call to
     * setException() will be a no-op because the Future is already done.
     *
     * Both scenarios are bad: The output Future might never complete, or, if it does complete, it
     * might not run some of its listeners. The likely result is that the app will hang. (And of
     * course stack overflows are bad news in general. For example, we may have overflowed in the
     * middle of defining a class. If so, that class will never be loadable in this process.) The
     * best we can do (since logging may overflow the stack) is to let the error propagate. Because
     * it is an Error, it won't be caught and logged by AbstractFuture.executeListener. Instead, it
     * can propagate through many layers of AbstractTransformFuture up to the root call to set().
     *
     * https://github.com/google/guava/issues/2254
     *
     * Other kinds of Errors are possible:
     *
     * - OutOfMemoryError from allocations in setFuture(): The calculus here is similar to
     * StackOverflowError: We can't reliably call setException(error).
     *
     * - Any kind of Error from a listener. Even if we could distinguish that case (by exposing some
     * extra state from AbstractFuture), our options are limited: A call to setException() would be
     * a no-op. We could log, but if that's what we really want, we should modify
     * AbstractFuture.executeListener to do so, since that method would have the ability to continue
     * to execute other listeners.
     *
     * What about RuntimeException? If there is a bug in set()/setValue() that produces one, it will
     * propagate, too, but only as far as AbstractFuture.executeListener, which will catch and log
     * it.
     */
    setResult(transformResult);
  }

  /** Template method for subtypes to actually run the transform. */
  abstract T doTransform(F function, I result);

  /** Template method for subtypes to actually set the result. */
  abstract void setResult(T result);

  @Override
  protected final void afterDone() {
    maybePropagateCancellation(inputFuture);
    this.inputFuture = null;
    this.function = null;
  }

  /**
   * An {@link AbstractTransformFuture} that delegates to a {@link Function} and
   * {@link #set(Object)}.
   */
  private static final class TransformFuture<I, O>
      extends AbstractTransformFuture<I, O, Function<? super I, ? extends O>, O> {
    TransformFuture(
        ListenableFuture<? extends I> inputFuture, Function<? super I, ? extends O> function) {
      super(inputFuture, function);
    }

    @Override
    O doTransform(Function<? super I, ? extends O> function, I input) {
      return function.apply(input);
      // TODO(lukes): move the UndeclaredThrowable catch block here?
    }

    @Override
    void setResult(O result) {
      set(result);
    }
  }
}
