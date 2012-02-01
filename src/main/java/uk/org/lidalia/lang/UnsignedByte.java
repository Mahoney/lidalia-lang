package uk.org.lidalia.lang;

import org.apache.commons.lang3.Validate;

public class UnsignedByte extends RichObject {
	
	public static UnsignedByte UnsignedByte(int theByte) {
		return new UnsignedByte(theByte);
	}
	
	@Identity private final int theByte;

	private UnsignedByte(int theByte) {
		Validate.inclusiveBetween(0, 255, theByte);
		this.theByte = theByte;
	}
	
	public int value() {
		return theByte;
	}
	
	public String toString() {
		return String.valueOf(theByte);
	}
}
