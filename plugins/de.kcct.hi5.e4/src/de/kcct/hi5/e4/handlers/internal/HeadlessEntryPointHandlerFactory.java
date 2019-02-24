package de.kcct.hi5.e4.handlers.internal;

import java.util.concurrent.CountDownLatch;

import org.osgi.service.component.annotations.Component;

import de.kcct.hi5.e4.EntryPointHandler;
import de.kcct.hi5.e4.EntryPointHandlerFactory;

@Component(property = EntryPointHandlerFactory.KEY + "=" + HeadlessEntryPointHandlerFactory.HEADLESS)
public class HeadlessEntryPointHandlerFactory implements EntryPointHandlerFactory {
	public static final String HEADLESS = "headless";
	private CountDownLatch stopLatch = new CountDownLatch(1);

	@Override
	public EntryPointHandler create(Context context) {
		return new EntryPointHandler() {
			@Override
			public void start() {
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
		};
	}
}
