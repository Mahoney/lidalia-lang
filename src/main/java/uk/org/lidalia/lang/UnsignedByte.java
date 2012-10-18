package uk.org.lidalia.lang;

import org.apache.commons.lang3.Validate;

public class UnsignedByte extends RichObject {

    private static final int MAX_BYTE_VALUE = 255;

    public static UnsignedByte from(int theByte) {
        return new UnsignedByte(theByte);
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
