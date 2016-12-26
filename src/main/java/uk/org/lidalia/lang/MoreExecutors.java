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

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

/**
 * Factory and utility methods for {@link Executor}, {@link ExecutorService},
 * and {@link ThreadFactory}.
 *
 * @author Eric Fellheimer
 * @author Kyle Littlefield
 * @author Justin Mahoney
 * @since 3.0
 */
final class MoreExecutors {
  private MoreExecutors() {}

  /**
   * Returns an {@link Executor} that runs each task in the thread that invokes
   * {@link Executor#execute execute}, as in {@link CallerRunsPolicy}.
   *
   * <p>This instance is equivalent to: <pre>   {@code
   *   final class DirectExecutor implements Executor {
   *     public void execute(Runnable r) {
   *       r.run();
   *     }
   *   }}</pre>
   *
   * @since 18.0
   */
  static Executor directExecutor() {
    return DirectExecutor.INSTANCE;
  }

  /** See {@link #directExecutor} for behavioral notes. */
  private enum DirectExecutor implements Executor {
    INSTANCE;

    @Override
    public void execute(Runnable command) {
      command.run();
    }

    @Override
    public String toString() {
      return "MoreExecutors.directExecutor()";
    }
  }

}
