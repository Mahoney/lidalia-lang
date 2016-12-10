package uk.org.lidalia.lang.encoding;

import uk.org.lidalia.lang.Bytes;

public interface Encoded<T extends Encoded<T>> {

    Encoder<T> encoder();

    Bytes decode();

}
