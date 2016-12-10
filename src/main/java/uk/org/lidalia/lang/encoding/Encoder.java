package uk.org.lidalia.lang.encoding;

import uk.org.lidalia.lang.Bytes;

import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.UTF_8;

public interface Encoder<T extends Encoded<T>> {

    T of(String encoded);

    T encode(Bytes decoded);

    default T encode(byte[] decoded) {
        return encode(Bytes.of(decoded));
    }

    default T encode(String decoded) {
        return encode(decoded, UTF_8);
    }

    default T encode(String decoded, Charset charset) {
        return encode(Bytes.of(decoded, charset));
    }
}
