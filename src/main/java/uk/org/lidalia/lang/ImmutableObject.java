package uk.org.lidalia.lang;

import java.util.concurrent.Callable;

public class ImmutableObject<I extends ImmutableObject<I>> extends RichObject implements Immutable<I> {

    private final LazyValue<Integer> lazilyInitialisedHashCode = new LazyValue<>(new Callable<Integer>() {
        @Override
        public Integer call() throws Exception {
            return ImmutableObject.super.hashCode();
        }
    });

    @Override
    public int hashCode() {
        return lazilyInitialisedHashCode.call();
    }

    private final LazyValue<String> lazilyInitialisedToString = new LazyValue<>(new Callable<String>() {
        @Override
        public String call() throws Exception {
            return ImmutableObject.super.toString();
        }
    });

    @Override
    public String toString() {
        return lazilyInitialisedToString.call();
    }

    @Override
    public I toImmutable() {
        return (I) this;
    }
}
