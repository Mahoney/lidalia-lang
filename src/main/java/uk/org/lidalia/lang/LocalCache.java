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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Objects.requireNonNull;
import static uk.org.lidalia.lang.Exceptions.throwUnchecked;
import static uk.org.lidalia.lang.Uninterruptibles.getUninterruptibly;

class LocalCache<K, V> {

    private static final int UNSET_INT = -1;

  /*
   * The basic strategy is to subdivide the table among Segments, each of which itself is a
   * concurrently readable hash table. The map supports non-blocking reads and concurrent writes
   * across different segments.
   *
   * If a maximum size is specified, a best-effort bounding is performed per segment, using a
   * page-replacement algorithm to determine which entries to evict when the capacity has been
   * exceeded.
   *
   * The page replacement algorithm's data structures are kept casually consistent with the map. The
   * ordering of writes to a segment is sequentially consistent. An update to the map and recording
   * of reads may not be immediately reflected on the algorithm's data structures. These structures
   * are guarded by a lock and operations are applied in batches to avoid lock contention. The
   * penalty of applying the batches is spread across threads so that the amortized cost is slightly
   * higher than performing just the operation without enforcing the capacity constraint.
   *
   * This implementation uses a per-segment queue to record a memento of the additions, removals,
   * and accesses that were performed on the map. The queue is drained on writes and when it exceeds
   * its capacity threshold.
   *
   * The Least Recently Used page replacement algorithm was chosen due to its simplicity, high hit
   * rate, and ability to be implemented with O(1) time complexity. The initial LRU implementation
   * operates per-segment rather than globally for increased implementation simplicity. We expect
   * the cache hit rate to be similar to that of a global LRU algorithm.
   */

  // Constants

  /**
   * The maximum capacity, used if a higher value is implicitly specified by either of the
   * constructors with arguments. MUST be a power of two <= 1<<30 to ensure that entries are
   * indexable using ints.
   */
  private static final int MAXIMUM_CAPACITY = 1 << 30;

  /**
   * Number of cache access operations that can be buffered per segment before the cache's recency
   * ordering information is updated. This is used to avoid lock contention by recording a memento
   * of reads and delaying a lock acquisition until the threshold is crossed or a mutation occurs.
   *
   * <p>This must be a (2^n)-1 as it is used as a mask.
   */
  private static final int DRAIN_THRESHOLD = 0x3F;

  /**
   * Maximum number of entries to be drained in a single cleanup run. This applies independently to
   * the cleanup queue and both reference queues.
   */
  // TODO(fry): empirically optimize this
  private static final int DRAIN_MAX = 16;

  /**
   * Mask value for indexing into segments. The upper bits of a key's hash code are used to choose
   * the segment.
   */
  private final int segmentMask;

  /**
   * Shift value for indexing within segments. Helps prevent entries that end up in the same segment
   * from also ending up in the same bucket.
   */
  private final int segmentShift;

  /** The segments, each of which is a specialized hash table. */
  private final Segment<K, V>[] segments;

  /** Factory used to create new entries. */
  private final EntryFactory entryFactory;

  /**
   * The default cache loader to use on loading operations.
   */
  private final CacheLoader<? super K, V> defaultLoader;

  /**
   * Creates a new, empty map with the specified strategy, initial capacity and concurrency level.
   */
  private LocalCache(
          CacheLoader<? super K, V> loader) {
    /* The concurrency level. */
    int concurrencyLevel = 4;

    entryFactory = EntryFactory.WEAK;
    defaultLoader = loader;

    int initialCapacity = 16;

    // Find the lowest power-of-two segmentCount that exceeds concurrencyLevel, unless
    // maximumSize/Weight is specified in which case ensure that each segment gets at least 10
    // entries. The special casing for size-based eviction is only necessary because that eviction
    // happens per segment instead of globally, so too many segments compared to the maximum size
    // will result in random eviction behavior.
    int segmentShift = 0;
    int segmentCount = 1;
    while (segmentCount < concurrencyLevel) {
      ++segmentShift;
      segmentCount <<= 1;
    }
    this.segmentShift = 32 - segmentShift;
    segmentMask = segmentCount - 1;

    this.segments = newSegmentArray(segmentCount);

    int segmentCapacity = initialCapacity / segmentCount;
    if (segmentCapacity * segmentCount < initialCapacity) {
      ++segmentCapacity;
    }

    int segmentSize = 1;
    while (segmentSize < segmentCapacity) {
      segmentSize <<= 1;
    }

    for (int i = 0; i < this.segments.length; ++i) {
      this.segments[i] =
          createSegment(segmentSize);
    }
  }

  private static <T> boolean equivalent(T a, T b) {
    if (a == b) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }
    return false;
  }

  /**
   * Creates new entries.
   */
  enum EntryFactory {
    WEAK {
      @Override
      <K, V> ReferenceEntry<K, V> newEntry(
          Segment<K, V> segment, K key, int hash, ReferenceEntry<K, V> next) {
        return new WeakEntry<>(segment.keyReferenceQueue, key, hash, next);
      }
    };

    /**
     * Creates a new entry.
     *
     * @param segment to create the entry for
     * @param key of the entry
     * @param hash of the key
     * @param next entry in the same bucket
     */
    abstract <K, V> ReferenceEntry<K, V> newEntry(
        Segment<K, V> segment, K key, int hash, ReferenceEntry<K, V> next);

    /**
     * Copies an entry, assigning it a new {@code next} entry.
     *
     * @param original the entry to copy
     * @param newNext entry in the same bucket
     */
    // Guarded By Segment.this
    <K, V> ReferenceEntry<K, V> copyEntry(
        Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
      return newEntry(segment, original.getKey(), original.getHash(), newNext);
    }

  }

  /**
   * A reference to a value.
   */
  interface ValueReference<K, V> {
    /**
     * Returns the value. Does not block or throw exceptions.
     */
    V get();

    /**
     * Waits for a value that may still be loading. Unlike get(), this method can block (in the case
     * of FutureValueReference).
     *
     * @throws ExecutionException if the loading thread throws an exception
     */
    V waitForValue() throws ExecutionException;

      /**
     * Returns the entry associated with this value reference, or {@code null} if this value
     * reference is independent of any entry.
     */
    ReferenceEntry<K, V> getEntry();

    /**
     * Creates a copy of this reference for the given entry.
     *
     * <p>{@code value} may be null only for a loading reference.
     */
    ValueReference<K, V> copyFor(
            ReferenceQueue<V> queue, V value, ReferenceEntry<K, V> entry);

    /**
     * Notifify pending loads that a new value was set. This is only relevant to loading value
     * references.
     */
    void notifyNewValue(V newValue);

    /**
     * Returns true if a new value is currently loading, regardless of whether or not there is an
     * existing value. It is assumed that the return value of this method is constant for any given
     * ValueReference instance.
     */
    boolean isLoading();

    /**
     * Returns true if this reference contains an active value, meaning one that is still considered
     * present in the cache. Active values consist of live values, which are returned by cache
     * lookups, and dead values, which have been evicted but awaiting removal. Non-active values
     * consist strictly of loading values, though during refresh a value may be both active and
     * loading.
     */
    boolean isActive();
  }

  /**
   * Placeholder. Indicates that the value hasn't been set yet.
   */
  private static final ValueReference<Object, Object> UNSET =
      new ValueReference<Object, Object>() {
        @Override
        public Object get() {
          return null;
        }

          @Override
        public ReferenceEntry<Object, Object> getEntry() {
          return null;
        }

        @Override
        public ValueReference<Object, Object> copyFor(
            ReferenceQueue<Object> queue,
            Object value,
            ReferenceEntry<Object, Object> entry) {
          return this;
        }

        @Override
        public boolean isLoading() {
          return false;
        }

        @Override
        public boolean isActive() {
          return false;
        }

        @Override
        public Object waitForValue() {
          return null;
        }

        @Override
        public void notifyNewValue(Object newValue) {}
      };

  /**
   * Singleton placeholder that indicates a value is being loaded.
   */
  @SuppressWarnings("unchecked") // impl never uses a parameter or returns any non-null value
  private static <K, V> ValueReference<K, V> unset() {
    return (ValueReference<K, V>) UNSET;
  }

  /**
   * An entry in a reference map.
   *
   * Entries in the map can be in the following states:
   *
   * Valid:
   *
   * - Live: valid key/value are set
   *
   * - Loading: loading is pending
   *
   * Invalid:
   *
   * - Expired: time expired (key/value may still be set)
   *
   * - Collected: key/value was partially collected, but not yet cleaned up
   *
   * - Unset: marked as unset, awaiting cleanup or reuse
   */
  interface ReferenceEntry<K, V> {
    /**
     * Returns the value reference from this entry.
     */
    ValueReference<K, V> getValueReference();

    /**
     * Sets the value reference for this entry.
     */
    void setValueReference(ValueReference<K, V> valueReference);

    /**
     * Returns the next entry in the chain.
     */
    ReferenceEntry<K, V> getNext();

    /**
     * Returns the entry's hash.
     */
    int getHash();

    /**
     * Returns the key for this entry.
     */
    K getKey();

  }

  /*
   * Note: All of this duplicate code sucks, but it saves a lot of memory. If only Java had mixins!
   * To maintain this code, make a change for the strong reference type. Then, cut and paste, and
   * replace "Strong" with "Soft" or "Weak" within the pasted text. The primary difference is that
   * strong entries store the key reference directly while soft and weak entries delegate to their
   * respective superclasses.
   */

  /**
   * Used for weakly-referenced keys.
   */
  static class WeakEntry<K, V> extends WeakReference<K> implements ReferenceEntry<K, V> {
    WeakEntry(ReferenceQueue<K> queue, K key, int hash, ReferenceEntry<K, V> next) {
      super(key, queue);
      this.hash = hash;
      this.next = next;
    }

    @Override
    public K getKey() {
      return get();
    }

    /*
     * It'd be nice to get these for free from AbstractReferenceEntry, but we're already extending
     * WeakReference<K>.
     */

    // null access

    // null write

    // The code below is exactly the same for each entry type.

    final int hash;
    final ReferenceEntry<K, V> next;
    volatile ValueReference<K, V> valueReference = unset();

    @Override
    public ValueReference<K, V> getValueReference() {
      return valueReference;
    }

    @Override
    public void setValueReference(ValueReference<K, V> valueReference) {
      this.valueReference = valueReference;
    }

    @Override
    public int getHash() {
      return hash;
    }

    @Override
    public ReferenceEntry<K, V> getNext() {
      return next;
    }
  }

  /**
   * References a soft value.
   */
  static class SoftValueReference<K, V> extends SoftReference<V> implements ValueReference<K, V> {
    final ReferenceEntry<K, V> entry;

    SoftValueReference(ReferenceQueue<V> queue, V referent, ReferenceEntry<K, V> entry) {
      super(referent, queue);
      this.entry = entry;
    }

      @Override
    public ReferenceEntry<K, V> getEntry() {
      return entry;
    }

    @Override
    public void notifyNewValue(V newValue) {}

    @Override
    public ValueReference<K, V> copyFor(
        ReferenceQueue<V> queue, V value, ReferenceEntry<K, V> entry) {
      return new SoftValueReference<>(queue, value, entry);
    }

    @Override
    public boolean isLoading() {
      return false;
    }

    @Override
    public boolean isActive() {
      return true;
    }

    @Override
    public V waitForValue() {
      return get();
    }
  }

    /**
   * Applies a supplemental hash function to a given hash code, which defends against poor quality
   * hash functions. This is critical when the concurrent hash map uses power-of-two length hash
   * tables, that otherwise encounter collisions for hash codes that do not differ in lower or upper
   * bits.
   *
   * @param h hash code
   */
  private static int rehash(int h) {
    // Spread bits to regularize both segment and index locations,
    // using variant of single-word Wang/Jenkins hash.
    // TODO(kevinb): use Hashing/move this to Hashing?
    h += (h << 15) ^ 0xffffcd7d;
    h ^= (h >>> 10);
    h += (h << 3);
    h ^= (h >>> 6);
    h += (h << 2) + (h << 14);
    return h ^ (h >>> 16);
  }

  private int hash(Object key) {
    int h = System.identityHashCode(key);
    return rehash(h);
  }

  void reclaimValue(ValueReference<K, V> valueReference) {
    ReferenceEntry<K, V> entry = valueReference.getEntry();
    int hash = entry.getHash();
    segmentFor(hash).reclaimValue(entry.getKey(), hash, valueReference);
  }

  void reclaimKey(ReferenceEntry<K, V> entry) {
    int hash = entry.getHash();
    segmentFor(hash).reclaimKey(entry, hash);
  }

  /**
   * Returns the segment that should be used for a key with the given hash.
   *
   * @param hash the hash code for the key
   * @return the segment
   */
  private Segment<K, V> segmentFor(int hash) {
    // TODO(fry): Lazily create segments?
    return segments[(hash >>> segmentShift) & segmentMask];
  }

  private Segment<K, V> createSegment(
          int initialCapacity) {
    return new Segment<>(this, initialCapacity, (long) UNSET_INT);
  }

  @SuppressWarnings("unchecked")
  private Segment<K, V>[] newSegmentArray(int ssize) {
    return new Segment[ssize];
  }

  // Inner Classes

  /**
   * Segments are specialized versions of hash tables. This subclass inherits from ReentrantLock
   * opportunistically, just to simplify some locking and avoid separate construction.
   */
  @SuppressWarnings("serial") // This class is never serialized.
  static class Segment<K, V> extends ReentrantLock {

    /*
     * TODO(fry): Consider copying variables (like evictsBySize) from outer class into this class.
     * It will require more memory but will reduce indirection.
     */

    /*
     * Segments maintain a table of entry lists that are ALWAYS kept in a consistent state, so can
     * be read without locking. Next fields of nodes are immutable (final). All list additions are
     * performed at the front of each bin. This makes it easy to check changes, and also fast to
     * traverse. When nodes would otherwise be changed, new nodes are created to replace them. This
     * works well for hash tables since the bin lists tend to be short. (The average length is less
     * than two.)
     *
     * Read operations can thus proceed without locking, but rely on selected uses of volatiles to
     * ensure that completed write operations performed by other threads are noticed. For most
     * purposes, the "count" field, tracking the number of elements, serves as that volatile
     * variable ensuring visibility. This is convenient because this field needs to be read in many
     * read operations anyway:
     *
     * - All (unsynchronized) read operations must first read the "count" field, and should not look
     * at table entries if it is 0.
     *
     * - All (synchronized) write operations should write to the "count" field after structurally
     * changing any bin. The operations must not take any action that could even momentarily cause a
     * concurrent read operation to see inconsistent data. This is made easier by the nature of the
     * read operations in Map. For example, no operation can reveal that the table has grown but the
     * threshold has not yet been updated, so there are no atomicity requirements for this with
     * respect to reads.
     *
     * As a guide, all critical volatile reads and writes to the count field are marked in code
     * comments.
     */

    final LocalCache<K, V> map;

    /**
     * The number of live elements in this segment's region.
     */
    volatile int count;

    /**
     * Number of updates that alter the size of the table. This is used during bulk-read methods to
     * make sure they see a consistent snapshot: If modCounts change during a traversal of segments
     * loading size or checking containsValue, then we might have an inconsistent view of state so
     * (usually) must retry.
     */
    int modCount;

    /**
     * The table is expanded when its size exceeds this threshold. (The value of this field is
     * always {@code (int) (capacity * 0.75)}.)
     */
    int threshold;

    /**
     * The per-segment table.
     */
    volatile AtomicReferenceArray<ReferenceEntry<K, V>> table;

    /**
     * The maximum weight of this segment. UNSET_INT if there is no maximum.
     */
    final long maxSegmentWeight;

    /**
     * The key reference queue contains entries whose keys have been garbage collected, and which
     * need to be cleaned up internally.
     */
    final ReferenceQueue<K> keyReferenceQueue;

    /**
     * The value reference queue contains value references whose values have been garbage collected,
     * and which need to be cleaned up internally.
     */
    final ReferenceQueue<V> valueReferenceQueue;

    /**
     * A counter of the number of reads since the last write, used to drain queues on a small
     * fraction of read operations.
     */
    final AtomicInteger readCount = new AtomicInteger();


    Segment(
            LocalCache<K, V> map,
            int initialCapacity,
            long maxSegmentWeight) {
      this.map = map;
      this.maxSegmentWeight = maxSegmentWeight;
      initTable(newEntryArray(initialCapacity));

        keyReferenceQueue = new ReferenceQueue<>();

        valueReferenceQueue = new ReferenceQueue<>();

    }

    AtomicReferenceArray<ReferenceEntry<K, V>> newEntryArray(int size) {
      return new AtomicReferenceArray<>(size);
    }

    void initTable(AtomicReferenceArray<ReferenceEntry<K, V>> newTable) {
      this.threshold = newTable.length() * 3 / 4; // 0.75
      if (this.threshold == maxSegmentWeight) {
        // prevent spurious expansion before eviction
        this.threshold++;
      }
      this.table = newTable;
    }

    ReferenceEntry<K, V> newEntry(K key, int hash, ReferenceEntry<K, V> next) {
      return map.entryFactory.newEntry(this, requireNonNull(key), hash, next);
    }

    /**
     * Copies {@code original} into a new entry chained to {@code newNext}. Returns the new entry,
     * or {@code null} if {@code original} was already garbage collected.
     */
    ReferenceEntry<K, V> copyEntry(ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
      if (original.getKey() == null) {
        // key collected
        return null;
      }

      ValueReference<K, V> valueReference = original.getValueReference();
      V value = valueReference.get();
      if ((value == null) && valueReference.isActive()) {
        // value collected
        return null;
      }

      ReferenceEntry<K, V> newEntry = map.entryFactory.copyEntry(this, original, newNext);
      newEntry.setValueReference(valueReference.copyFor(this.valueReferenceQueue, value, newEntry));
      return newEntry;
    }

    /**
     * Sets a new value of an entry. Adds newly created entries at the end of the access queue.
     */
    void setValue(ReferenceEntry<K, V> entry, V value) {
      ValueReference<K, V> previous = entry.getValueReference();

      ValueReference<K, V> valueReference =
              new SoftValueReference<>(this.valueReferenceQueue, value, entry);
      entry.setValueReference(valueReference);
      // we are already under lock, so drain the recency queue immediately
      previous.notifyNewValue(value);
    }

    // loading

    V get(K key, int hash, CacheLoader<? super K, V> loader) {
      requireNonNull(key);
      requireNonNull(loader);
      try {
        if (count != 0) { // read-volatile
          // don't call getLiveEntry, which would ignore loading values
          ReferenceEntry<K, V> e = getEntry(key, hash);
          if (e != null) {
            V value = getLiveValue(e);
            if (value != null) {

                return value;
            }
            ValueReference<K, V> valueReference = e.getValueReference();
            if (valueReference.isLoading()) {
              return waitForLoadingValue(key, valueReference);
            }
          }
        }

        // at this point e is either null or expired;
        return lockedGetOrLoad(key, hash, loader);
      } catch (ExecutionException ee) {
        return throwUnchecked(ee.getCause(), null);
      } finally {
        postReadCleanup();
      }
    }

    V lockedGetOrLoad(K key, int hash, CacheLoader<? super K, V> loader) throws ExecutionException {
      ReferenceEntry<K, V> e;
      ValueReference<K, V> valueReference = null;
      LoadingValueReference<K, V> loadingValueReference = null;
      boolean createNewEntry = true;

      lock();
      try {
        // re-read ticker once inside the lock
        runLockedCleanup();

        int newCount = this.count - 1;
        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        for (e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash
              && entryKey != null
              && equivalent(key, entryKey)) {
            valueReference = e.getValueReference();
            if (valueReference.isLoading()) {
              createNewEntry = false;
            } else {
              V value = valueReference.get();
              if (value != null) {

                  // we were concurrent with loading; don't consider refresh
                return value;
              }

              // immediately reuse invalid entries
              this.count = newCount; // write-volatile
            }
            break;
          }
        }

        if (createNewEntry) {
          loadingValueReference = new LoadingValueReference<>();

          if (e == null) {
            e = newEntry(key, hash, first);
            e.setValueReference(loadingValueReference);
            table.set(index, e);
          } else {
            e.setValueReference(loadingValueReference);
          }
        }
      } finally {
        unlock();
        // locked cleanup may generate notifications we can send unlocked
      }

      if (createNewEntry) {
        // Synchronizes on the entry to allow failing fast when a recursive load is
        // detected. This may be circumvented when an entry is copied, but will fail fast most
        // of the time.
        synchronized (e) {
          return loadSync(key, hash, loadingValueReference, loader);
        }
      } else {
        // The entry already exists. Wait for loading.
        return waitForLoadingValue(key, valueReference);
      }
    }

    V waitForLoadingValue(K key, ValueReference<K, V> valueReference)
        throws ExecutionException {
      if (!valueReference.isLoading()) {
        throw new AssertionError();
      }

      // don't consider expiration as we're concurrent with loading
      V value = valueReference.waitForValue();
      if (value == null) {
        throw new NullPointerException("CacheLoader returned null for key " + key + ".");
      }
      // re-read ticker now that loading has completed

        return value;
    }

    // at most one of loadSync/loadAsync may be called for any given LoadingValueReference

    V loadSync(
        K key,
        int hash,
        LoadingValueReference<K, V> loadingValueReference,
        CacheLoader<? super K, V> loader)
        throws ExecutionException {
      Future<V> loadingFuture = loadingValueReference.loadFuture(key, loader);
      return getAndRecordStats(key, hash, loadingValueReference, loadingFuture);
    }

      /**
     * Waits uninterruptibly for {@code newValue} to be loaded, and then records loading stats.
     */
    V getAndRecordStats(
        K key,
        int hash,
        LoadingValueReference<K, V> loadingValueReference,
        Future<V> newValue)
        throws ExecutionException {
      V value = null;
      try {
        value = getUninterruptibly(newValue);
        if (value == null) {
          throw new NullPointerException("CacheLoader returned null for key " + key + ".");
        }
        storeLoadedValue(key, hash, loadingValueReference, value);
        return value;
      } finally {
        if (value == null) {
          removeLoadingValue(key, hash, loadingValueReference);
        }
      }
    }

      // reference queues, for garbage collection cleanup

    /**
     * Cleanup collected entries when the lock is available.
     */
    void tryDrainReferenceQueues() {
      if (tryLock()) {
        try {
          drainReferenceQueues();
        } finally {
          unlock();
        }
      }
    }

    /**
     * Drain the key and value reference queues, cleaning up internal entries containing garbage
     * collected keys or values.
     */
    void drainReferenceQueues() {
        drainKeyReferenceQueue();
        drainValueReferenceQueue();
    }

    void drainKeyReferenceQueue() {
      Reference<? extends K> ref;
      int i = 0;
      while ((ref = keyReferenceQueue.poll()) != null) {
        @SuppressWarnings("unchecked")
        ReferenceEntry<K, V> entry = (ReferenceEntry<K, V>) ref;
        map.reclaimKey(entry);
        if (++i == DRAIN_MAX) {
          break;
        }
      }
    }

    void drainValueReferenceQueue() {
      Reference<? extends V> ref;
      int i = 0;
      while ((ref = valueReferenceQueue.poll()) != null) {
        @SuppressWarnings("unchecked")
        ValueReference<K, V> valueReference = (ValueReference<K, V>) ref;
        map.reclaimValue(valueReference);
        if (++i == DRAIN_MAX) {
          break;
        }
      }
    }

    /**
     * Returns first entry of bin for given hash.
     */
    ReferenceEntry<K, V> getFirst(int hash) {
      // read this volatile field only once
      AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
      return table.get(hash & (table.length() - 1));
    }

    // Specialized implementations of map methods

    ReferenceEntry<K, V> getEntry(Object key, int hash) {
      for (ReferenceEntry<K, V> e = getFirst(hash); e != null; e = e.getNext()) {
        if (e.getHash() != hash) {
          continue;
        }

        K entryKey = e.getKey();
        if (entryKey == null) {
          tryDrainReferenceQueues();
          continue;
        }

        if (equivalent(key, entryKey)) {
          return e;
        }
      }

      return null;
    }

    /**
     * Gets the value from an entry. Returns null if the entry is invalid, partially-collected,
     * loading, or expired.
     */
    V getLiveValue(ReferenceEntry<K, V> entry) {
      if (entry.getKey() == null) {
        tryDrainReferenceQueues();
        return null;
      }
      V value = entry.getValueReference().get();
      if (value == null) {
        tryDrainReferenceQueues();
        return null;
      }

      return value;
    }

    /**
     * Expands the table if possible.
     */
    void expand() {
      AtomicReferenceArray<ReferenceEntry<K, V>> oldTable = table;
      int oldCapacity = oldTable.length();
      if (oldCapacity >= MAXIMUM_CAPACITY) {
        return;
      }

      /*
       * Reclassify nodes in each list to new Map. Because we are using power-of-two expansion, the
       * elements from each bin must either stay at same index, or move with a power of two offset.
       * We eliminate unnecessary node creation by catching cases where old nodes can be reused
       * because their next fields won't change. Statistically, at the default threshold, only about
       * one-sixth of them need cloning when a table doubles. The nodes they replace will be garbage
       * collectable as soon as they are no longer referenced by any reader thread that may be in
       * the midst of traversing table right now.
       */

      int newCount = count;
      AtomicReferenceArray<ReferenceEntry<K, V>> newTable = newEntryArray(oldCapacity << 1);
      threshold = newTable.length() * 3 / 4;
      int newMask = newTable.length() - 1;
      for (int oldIndex = 0; oldIndex < oldCapacity; ++oldIndex) {
        // We need to guarantee that any existing reads of old Map can
        // proceed. So we cannot yet null out each bin.
        ReferenceEntry<K, V> head = oldTable.get(oldIndex);

        if (head != null) {
          ReferenceEntry<K, V> next = head.getNext();
          int headIndex = head.getHash() & newMask;

          // Single node on list
          if (next == null) {
            newTable.set(headIndex, head);
          } else {
            // Reuse the consecutive sequence of nodes with the same target
            // index from the end of the list. tail points to the first
            // entry in the reusable list.
            ReferenceEntry<K, V> tail = head;
            int tailIndex = headIndex;
            for (ReferenceEntry<K, V> e = next; e != null; e = e.getNext()) {
              int newIndex = e.getHash() & newMask;
              if (newIndex != tailIndex) {
                // The index changed. We'll need to copy the previous entry.
                tailIndex = newIndex;
                tail = e;
              }
            }
            newTable.set(tailIndex, tail);

            // Clone nodes leading up to the tail.
            for (ReferenceEntry<K, V> e = head; e != tail; e = e.getNext()) {
              int newIndex = e.getHash() & newMask;
              ReferenceEntry<K, V> newNext = newTable.get(newIndex);
              ReferenceEntry<K, V> newFirst = copyEntry(e, newNext);
              if (newFirst != null) {
                newTable.set(newIndex, newFirst);
              } else {
                newCount--;
              }
            }
          }
        }
      }
      table = newTable;
      this.count = newCount;
    }

      void storeLoadedValue(
        K key, int hash, LoadingValueReference<K, V> oldValueReference, V newValue) {
      lock();
      try {
        runLockedCleanup();

        int newCount = this.count + 1;
        if (newCount > this.threshold) { // ensure capacity
          expand();
          newCount = this.count + 1;
        }

        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash
              && entryKey != null
              && equivalent(key, entryKey)) {
            ValueReference<K, V> valueReference = e.getValueReference();
            V entryValue = valueReference.get();
            // replace the old LoadingValueReference if it's live, otherwise
            // perform a putIfAbsent
            if (oldValueReference == valueReference
                || (entryValue == null && valueReference != UNSET)) {
              ++modCount;
              if (oldValueReference.isActive()) {
                newCount--;
              }
              setValue(e, newValue);
              this.count = newCount; // write-volatile
                return;
            }

            // the loaded value was already clobbered
              return;
          }
        }

        ++modCount;
        ReferenceEntry<K, V> newEntry = newEntry(key, hash, first);
        setValue(newEntry, newValue);
        table.set(index, newEntry);
        this.count = newCount; // write-volatile
      } finally {
        unlock();
        // locked cleanup may generate notifications we can send unlocked
      }
    }

    ReferenceEntry<K, V> removeValueFromChain(
            ReferenceEntry<K, V> first,
            ReferenceEntry<K, V> entry,
            ValueReference<K, V> valueReference) {

        if (valueReference.isLoading()) {
        valueReference.notifyNewValue(null);
        return first;
      } else {
        return removeEntryFromChain(first, entry);
      }
    }

    ReferenceEntry<K, V> removeEntryFromChain(
        ReferenceEntry<K, V> first, ReferenceEntry<K, V> entry) {
      int newCount = count;
      ReferenceEntry<K, V> newFirst = entry.getNext();
      for (ReferenceEntry<K, V> e = first; e != entry; e = e.getNext()) {
        ReferenceEntry<K, V> next = copyEntry(e, newFirst);
        if (next != null) {
          newFirst = next;
        } else {
          newCount--;
        }
      }
      this.count = newCount;
      return newFirst;
    }

    /**
     * Removes an entry whose key has been garbage collected.
     */
    void reclaimKey(ReferenceEntry<K, V> entry, int hash) {
      lock();
      try {
        int newCount;
        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
          if (e == entry) {
            ++modCount;
            ReferenceEntry<K, V> newFirst =
                removeValueFromChain(
                    first,
                    e,
                        e.getValueReference()
                );
            newCount = this.count - 1;
            table.set(index, newFirst);
            this.count = newCount; // write-volatile
              return;
          }
        }
      } finally {
        unlock();
        // locked cleanup may generate notifications we can send unlocked
      }
    }

    /**
     * Removes an entry whose value has been garbage collected.
     */
    void reclaimValue(K key, int hash, ValueReference<K, V> valueReference) {
      lock();
      try {
        int newCount;
        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash
              && entryKey != null
              && equivalent(key, entryKey)) {
            ValueReference<K, V> v = e.getValueReference();
            if (v == valueReference) {
              ++modCount;
              ReferenceEntry<K, V> newFirst =
                  removeValueFromChain(
                      first,
                      e,
                          valueReference
                  );
              newCount = this.count - 1;
              table.set(index, newFirst);
              this.count = newCount; // write-volatile
                return;
            }
              return;
          }
        }
      } finally {
        unlock();
      }
    }

    void removeLoadingValue(K key, int hash, LoadingValueReference<K, V> valueReference) {
      lock();
      try {
        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash
              && entryKey != null
              && equivalent(key, entryKey)) {
            ValueReference<K, V> v = e.getValueReference();
            if (v == valueReference) {
              if (valueReference.isActive()) {
                e.setValueReference(valueReference.getOldValue());
              } else {
                ReferenceEntry<K, V> newFirst = removeEntryFromChain(first, e);
                table.set(index, newFirst);
              }
                return;
            }
              return;
          }
        }
      } finally {
        unlock();
        // locked cleanup may generate notifications we can send unlocked
      }
    }

    /**
     * Performs routine cleanup following a read. Normally cleanup happens during writes. If cleanup
     * is not observed after a sufficient number of reads, try cleaning up from the read thread.
     */
    void postReadCleanup() {
      if ((readCount.incrementAndGet() & DRAIN_THRESHOLD) == 0) {
        runLockedCleanup();
      }
    }

    void runLockedCleanup() {
      if (tryLock()) {
        try {
          drainReferenceQueues();
            readCount.set(0);
        } finally {
          unlock();
        }
      }
    }

  }

  static class LoadingValueReference<K, V> implements ValueReference<K, V> {
    volatile ValueReference<K, V> oldValue;

    // TODO(fry): rename get, then extend AbstractFuture instead of containing SettableFuture
    final SettableFuture<V> futureValue = SettableFuture.create();

    LoadingValueReference() {
      this(LocalCache.unset());
    }

    LoadingValueReference(ValueReference<K, V> oldValue) {
      this.oldValue = oldValue;
    }

    @Override
    public boolean isLoading() {
      return true;
    }

    @Override
    public boolean isActive() {
      return oldValue.isActive();
    }

    boolean set(V newValue) {
      return futureValue.set(newValue);
    }

    boolean setException(Throwable t) {
      return futureValue.setException(t);
    }

    @Override
    public void notifyNewValue(V newValue) {
      if (newValue != null) {
        // The pending load was clobbered by a manual write.
        // Unblock all pending gets, and have them return the new value.
        set(newValue);
      } else {
        // The pending load was removed. Delay notifications until loading completes.
        oldValue = unset();
      }
    }

    Future<V> loadFuture(K key, CacheLoader<? super K, V> loader) {
      try {
        V newValue = loader.load(key);
        return set(newValue) ? futureValue : new ImmediateFuture.ImmediateSuccessfulFuture<>(newValue);
      } catch (Throwable t) {
        return setException(t) ? futureValue : new ImmediateFuture.ImmediateFailedFuture<>(t);
      }
    }

    @Override
    public V waitForValue() throws ExecutionException {
      return getUninterruptibly(futureValue);
    }

    @Override
    public V get() {
      return oldValue.get();
    }

    ValueReference<K, V> getOldValue() {
      return oldValue;
    }

    @Override
    public ReferenceEntry<K, V> getEntry() {
      return null;
    }

    @Override
    public ValueReference<K, V> copyFor(
        ReferenceQueue<V> queue, V value, ReferenceEntry<K, V> entry) {
      return this;
    }
  }

  private V get(K key, CacheLoader<? super K, V> loader) {
    int hash = hash(requireNonNull(key));
    return segmentFor(hash).get(key, hash, loader);
  }

  V getOrLoad(K key) {
    return get(key, defaultLoader);
  }

  // Serialization Support
  static class LocalManualCache<K, V> {
    final LocalCache<K, V> localCache;

    private LocalManualCache(LocalCache<K, V> localCache) {
      this.localCache = localCache;
    }

    // Cache methods

  }

  static class LocalLoadingCache<K, V> extends LocalManualCache<K, V>
      implements LoadingCache<K, V> {

    LocalLoadingCache(
            CacheLoader<? super K, V> loader) {
      super(new LocalCache<>(requireNonNull(loader)));
    }

    // LoadingCache methods

    @Override
    public V get(K key) throws ExecutionException {
      return localCache.getOrLoad(key);
    }

    @Override
    public V getUnchecked(K key) {
      try {
        return get(key);
      } catch (ExecutionException e) {
        return throwUnchecked(e.getCause(), null);
      }
    }

  }

}
