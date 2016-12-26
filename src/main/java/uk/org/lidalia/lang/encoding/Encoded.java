package uk.org.lidalia.lang.encoding;

import uk.org.lidalia.lang.Bytes;

import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.UTF_8;

public interface Encoded<T extends Encoded<T>> {

    Encoder<T> encoder();

    Bytes decode();

    default Bytes bytes(Charset charset) {
        return Bytes.of(toString(), charset);
    }

    default Bytes bytes() {
        return bytes(UTF_8);
    }

}
