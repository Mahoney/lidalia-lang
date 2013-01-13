package uk.org.lidalia.lang;

import org.apache.commons.lang3.Validate;

import static java.lang.Integer.parseInt;

public class UnsignedByte extends RichObject {

    private static final int MAX_BYTE_VALUE = 255;

    public static UnsignedByte UnsignedByte(int theByte) {
        return new UnsignedByte(theByte);
    }

    public static UnsignedByte UnsignedByte(String theByte) {
        return UnsignedByte(parseInt(theByte));
    }

    @Identity private final int theByte;

    private UnsignedByte(int theByte) {
        Validate.inclusiveBetween(0, MAX_BYTE_VALUE, theByte);
        this.theByte = theByte;
    }

    public int value() {
        return theByte;
    }

    public String toString() {
        return String.valueOf(theByte);
    }
}
