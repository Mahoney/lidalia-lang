package uk.org.lidalia.lang;

public final class Utils {

	public static byte[] copyOf(byte[] original) {
		byte[] newBytes = new byte[original.length];
		System.arraycopy(original, 0, newBytes, 0, original.length);
		return newBytes;
	}

	private Utils() {
		throw new UnsupportedOperationException("Not instantiable");
	}
}
