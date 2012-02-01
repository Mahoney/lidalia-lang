package uk.org.lidalia.lang;

public final class Utils {

	public static <T> T valueOrDefault(T valueWhichMightBeNull, T defaultValue) {
		if (valueWhichMightBeNull == null) {
			return defaultValue;
		} else {
			return valueWhichMightBeNull;
		}
	}

	public static byte[] copy(byte[] original) {
		byte[] newBytes = new byte[original.length];
		System.arraycopy(original, 0, newBytes, 0, original.length);
		return newBytes;
	}

	private Utils() {
		throw new UnsupportedOperationException("Not instantiable");
	}
}
