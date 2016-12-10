package uk.org.lidalia.lang.encoding.base64;

import uk.org.lidalia.lang.Bytes;
import uk.org.lidalia.lang.encoding.EncodedBase;

public class Base64Url extends EncodedBase<Base64Url> {

    Base64Url(String encoded, Base64UrlEncoder encoder) {
        super(encoded, encoder);
    }

    @Override
    public Bytes decode() {
        return Bytes.of(java.util.Base64.getUrlDecoder().decode(toString()));
    }
}
