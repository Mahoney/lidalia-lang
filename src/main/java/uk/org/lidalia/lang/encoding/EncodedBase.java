package uk.org.lidalia.lang.encoding;

import uk.org.lidalia.lang.WrappedString;

public abstract class EncodedBase<T extends EncodedBase<T>> extends WrappedString<T> implements Encoded<T> {

    private final Encoder<T> encoder;

    protected EncodedBase(String encoded, Encoder<T> encoder) {
        super(encoded);
        this.encoder = encoder;
    }

    @Override
    public final Encoder<T> encoder() {
        return encoder;
    }

}
