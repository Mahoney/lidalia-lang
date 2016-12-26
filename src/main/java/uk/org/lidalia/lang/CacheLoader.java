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

/**
 * Computes or retrieves values, based on a key, for use in populating a {@link LoadingCache}.
 *
 * <p>Most implementations will only need to implement {@link #load}. Other methods may be
 * overridden as desired.
 *
 * <p>Usage example: <pre>   {@code
 *
 *   CacheLoader<Key, Graph> loader = new CacheLoader<Key, Graph>() {
 *     public Graph load(Key key) throws AnyException {
 *       return createExpensiveGraph(key);
 *     }
 *   };
 *   LoadingCache<Key, Graph> cache = CacheBuilder.newBuilder().build(loader);}</pre>
 *
 * @author Charles Fry
 * @since 10.0
 */
interface CacheLoader<K, V> {

  V load(K key);

}
