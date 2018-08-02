package de.kcct.hi5.e4;

import java.net.URL;

import org.eclipse.equinox.app.IApplicationContext;

public interface EntryPointHandler {
	String KEY = "hi5.entrypoint.handler";

	void start(IApplicationContext context, URL entryPoint);

	void stop();
}
