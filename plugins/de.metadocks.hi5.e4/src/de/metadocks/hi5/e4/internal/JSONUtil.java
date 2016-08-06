package de.metadocks.hi5.e4.internal;

import java.util.function.Supplier;

public class JSONUtil {

	public static <T> T get(Supplier<T> func) {
		try {
			return func.get();
		} catch (Exception e) {
			// property does not exist, ignore
		}

		return null;
	}

}
