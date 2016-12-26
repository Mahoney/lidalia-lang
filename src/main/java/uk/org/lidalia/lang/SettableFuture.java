/*
 * Copyright (C) 2009 The Guava Authors
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

final class SettableFuture<V> extends AbstractFuture.TrustedFuture<V> {
  /**
   * Creates a new {@code SettableFuture} that can be completed or cancelled by a later method call.
   */
  static <V> SettableFuture<V> create() {
    return new SettableFuture<>();
  }

  @Override
  public boolean set(V value) {
    return super.set(value);
  }

  @Override
  public boolean setException(Throwable throwable) {
    return super.setException(throwable);
  }

  private SettableFuture() {}
}
