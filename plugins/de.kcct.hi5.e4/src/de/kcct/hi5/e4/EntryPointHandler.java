package de.kcct.hi5.e4;

import java.net.MalformedURLException;
import java.net.URL;

import de.kcct.hi5.e4.EntryPointHandlerFactory.Context;

public interface EntryPointHandler {
	void start();

	void stop();

	default URL calcURL(Context context) {
		String entryPoint = context.getProperty("entryPoint", "app");
		String base = "/" + entryPoint;
		String entryPointIndexFile = context.getProperty("entryPointIndexFile", "index.html");
		String alias = base + entryPointIndexFile;
		String protocol = System.getProperty("org.osgi.service.http.protocol", context.getProperty("protocol", "http"));
		String server = System.getProperty("org.osgi.service.http.host", context.getProperty("server", "localhost"));
		String port = System.getProperty("org.osgi.service.http.port", context.getProperty("port", "80"));
		String urlStr = String.format("%s://%s:%s%s", protocol, server, port, alias);
		try {
			return new URL(urlStr);
		} catch (MalformedURLException e) {
			return null;
		}
	}
}
