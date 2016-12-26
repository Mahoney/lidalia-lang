package uk.org.lidalia.lang.encoding;

import uk.org.lidalia.lang.WrappedString;

import static java.util.Objects.requireNonNull;

public abstract class EncodedBase<T extends EncodedBase<T>> extends WrappedString<T> implements Encoded<T> {

    private final Encoder<T> encoder;

    protected EncodedBase(String encoded, Encoder<T> encoder) {
        super(encoded);
        this.encoder = requireNonNull(encoder);
    }

    @Override
    public final Encoder<T> encoder() {
        return encoder;
    }

}
