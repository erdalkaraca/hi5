/*******************************************************************************
 * Copyright (c) 2016 Erdal Karaca.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Erdal Karaca <erdal.karaca.de@gmail.com> - initial API and implementation
 *******************************************************************************/
package de.kcct.hi5.e4.internal;

import java.util.Collection;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import de.kcct.hi5.e4.EntryPointHandler;
import de.kcct.hi5.e4.EntryPointHandlerFactory;
import de.kcct.hi5.e4.handlers.internal.HeadlessEntryPointHandlerFactory;

public class E4EquinoxApp implements IApplication {

	private EntryPointHandler handler;

	@Override
	public final Object start(IApplicationContext context) throws Exception {
		Bundle bundle = FrameworkUtil.getBundle(E4EquinoxApp.class);
		BundleContext bundleContext = bundle.getBundleContext();
		WebResourcesRegistry resReg = getService(bundleContext, WebResourcesRegistry.class, null);
		String entryPoint = context.getBrandingProperty("entryPoint");
		String base = "/" + entryPoint;
		resReg.registerResources(base);
		E4Runtime e4runtime = getService(bundleContext, E4Runtime.class, null);
		e4runtime.createE4Workbench(context);

		context.applicationRunning();
		String entryPointHandler = context.getBrandingProperty(EntryPointHandlerFactory.KEY);
		if (entryPointHandler == null) {
			entryPointHandler = System.getProperty(EntryPointHandlerFactory.KEY,
					HeadlessEntryPointHandlerFactory.HEADLESS);
		}
		String filter = String.format("(%s=%s)", EntryPointHandlerFactory.KEY, entryPointHandler);
		EntryPointHandlerFactory factory = getService(bundleContext, EntryPointHandlerFactory.class, filter);
		handler = factory.create((key, defaultValue) -> {
			String value = context.getBrandingProperty(key);
			if (value == null) {
				value = defaultValue;
			}
			return value;
		});
		handler.start();
		resReg.unregisterAll(base);
		return IApplication.EXIT_OK;
	}

	private static <T> T getService(BundleContext bundleContext, Class<T> serviceType, String filter)
			throws InvalidSyntaxException {
		Collection<ServiceReference<T>> refs = bundleContext.getServiceReferences(serviceType, filter);
		if (refs.isEmpty()) {
			throw new IllegalArgumentException(
					String.format("No service of type '%s' found using filters '%s'", serviceType.getName(), filter));
		}
		return bundleContext.getService(refs.iterator().next());
	}

	@Override
	public final void stop() {
		handler.stop();
	}
}
