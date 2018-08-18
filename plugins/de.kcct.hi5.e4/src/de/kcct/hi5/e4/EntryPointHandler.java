package de.kcct.hi5.e4;

import java.net.URL;

public interface EntryPointHandler {
	void start(Context context, URL entryPoint);

	void stop();

	interface Context {
		String getProperty(String key);
	}
}
