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
package de.metadocks.hi5.e4.internal;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;

public class E4EquinoxApp implements IApplication {
	private String alias;
	private HttpService httpService;
	private String base;

	@Override
	public final Object start(IApplicationContext context) throws Exception {
		Bundle bundle = FrameworkUtil.getBundle(E4EquinoxApp.class);
		BundleContext bundleContext = bundle.getBundleContext();
		httpService = getService(bundleContext, HttpService.class);
		WebResourcesRegistry resReg = getService(bundleContext, WebResourcesRegistry.class);
		String entryPoint = context.getBrandingProperty("entryPoint");
		base = "/" + entryPoint;
		alias = base + "/index.html";
		resReg.registerResources(base);
		E4Runtime e4runtime = getService(bundleContext, E4Runtime.class);
		e4runtime.createE4Workbench(context);

		context.applicationRunning();
		boolean startJfxClient = Boolean.getBoolean("hi5-start-jfx-client");
		// startJfxClient = true;
		if (startJfxClient) {
			String url = String.format("http://localhost:8080%s", alias);
			// the embedded jfx browser windows should be started
			JFXBrowserApp.run(context.getBrandingName(), url);
		} else {
			// just wait until framework shuts down
			synchronized (this) {
				this.wait();
			}
		}
		resReg.unregisterAll(entryPoint);
		return IApplication.EXIT_OK;
	}

	private static <T> T getService(BundleContext bundleContext, Class<T> serviceType) {
		ServiceReference<T> ref = bundleContext.getServiceReference(serviceType);
		return bundleContext.getService(ref);
	}

	@Override
	public final void stop() {
		synchronized (this) {
			this.notify();
		}
	}
}
