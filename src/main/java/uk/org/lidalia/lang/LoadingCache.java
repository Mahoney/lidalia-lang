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

import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * A semi-persistent mapping from keys to values. Values are automatically loaded by the cache, and
 * are stored in the cache until either evicted or manually invalidated.
 *
 * <p>Implementations of this interface are expected to be thread-safe, and can be safely accessed
 * by multiple concurrent threads.
 *
 * <p>When evaluated as a {@link Function}, a cache yields the same result as invoking
 * {@link #getUnchecked}.
 *
 * @author Charles Fry
 * @since 11.0
 */
interface LoadingCache<K, V> {

  V get(K key) throws ExecutionException;

  V getUnchecked(K key);

}
