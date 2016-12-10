package uk.org.lidalia.lang.encoding.hex;

import uk.org.lidalia.lang.Bytes;
import uk.org.lidalia.lang.encoding.EncodedBase;
import uk.org.lidalia.lang.encoding.Encoder;

import java.util.regex.Pattern;

public class Hex extends EncodedBase<Hex> {

    private static final Pattern legalHexEncoding = Pattern.compile("([0-9a-fA-F]{2})*");

    Hex(String encoded, Encoder<Hex> encoder) {
        super(encoded, encoder);
        if (!legalHexEncoding.matcher(encoded).matches()) {
            throw new IllegalArgumentException(toString()+" is not a valid hex string");
        }
    }

    @Override
    public Bytes decode() {

        char[] chars = toString().toCharArray();
        byte[] decoded = new byte[chars.length / 2];

        for (int i = 0; i < decoded.length; i++) {
            int nibble1 = Character.digit(chars[i*2], 16);
            int nibble2 = Character.digit(chars[i*2+1], 16);
            decoded[i] = (byte) (nibble1*16+nibble2);
        }

        return Bytes.of(decoded);
    }
}
