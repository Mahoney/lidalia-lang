package uk.org.lidalia.lang;

import java.util.Iterator;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

public final class Collections3 {

	public static String toString(Iterable<?> collection, String delimiter) {
		return toString(collection, "[", delimiter, "]");
	}

	public static <T> String toString(Iterable<T> collection, String start, String delimiter, String end) {
		return toString(collection, start, delimiter, end, new Function<T, String>() {
			@Override
			public String apply(T object) {
				return object.toString();
			}
		});
	}

	public static <T> String toString(Iterable<T> collection, String start, String delimiter, String end, Function<T, String> transform) {
		StringBuilder builder = new StringBuilder(start);
		Iterable<String> asStrings = Iterables.transform(collection, transform);
		for (Iterator<String> iterator = asStrings.iterator(); iterator.hasNext();) {
			builder.append(iterator.next());
			if (iterator.hasNext()) {
				builder.append(delimiter);
			}
		}
		builder.append(end);
		return builder.toString();
	}

	private Collections3() {
		throw new UnsupportedOperationException("Not instantiable");
	}
}
