package uk.org.lidalia.lang.encoding.base64;

import uk.org.lidalia.lang.Bytes;
import uk.org.lidalia.lang.encoding.Encoder;

import static java.util.Base64.getEncoder;

public class Base64UrlEncoder implements Encoder<Base64Url> {

    public static final Base64UrlEncoder base64Url = new Base64UrlEncoder();

    private Base64UrlEncoder() {}

    @Override
    public Base64Url of(String encoded) {
        return new Base64Url(encoded, this);
    }

    @Override
    public Base64Url encode(Bytes decoded) {
        return of(getEncoder().encodeToString(decoded.array()));
    }
}
