package de.metadocks.hi5.e4.handlers.internal;

import java.net.URL;
import java.util.concurrent.CountDownLatch;

import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.service.component.annotations.Component;

import de.metadocks.hi5.e4.EntryPointHandler;

@Component(property = EntryPointHandler.KEY + "=" + HeadlessEntryPointHandler.HEADLESS)
public class HeadlessEntryPointHandler implements EntryPointHandler {
	public static final String HEADLESS = "headless";
	private CountDownLatch stopLatch = new CountDownLatch(1);

	@Override
	public void start(IApplicationContext context, URL entryPoint) {
		// just wait until framework shuts down
		try {
			stopLatch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		stopLatch.countDown();
	}
}
