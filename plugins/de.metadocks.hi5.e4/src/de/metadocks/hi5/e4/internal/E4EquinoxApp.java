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

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class E4EquinoxApp implements IApplication {
	static E4EquinoxApp SELF;

	IApplicationContext applicationContext;
	Object returnValue;

	private E4Runtime e4Runtime;
	private String alias;
	private HttpService httpService;
	private String base;

	public static class JFXApp extends Application {
		private E4EquinoxApp osgiApp = SELF;
		private IApplicationContext applicationContext;

		@Override
		public void start(final Stage primaryStage) throws Exception {
			this.applicationContext = this.osgiApp.applicationContext;
			this.osgiApp.doStart(this.applicationContext, JFXApp.this, primaryStage);
		}

		@Override
		public void stop() throws Exception {
			super.stop();
			this.osgiApp.returnValue = IApplication.EXIT_OK;
		}
	}

	@Override
	public final Object start(IApplicationContext context) throws Exception {
		SELF = this;
		this.applicationContext = context;
		Bundle bundle = FrameworkUtil.getBundle(E4EquinoxApp.class);
		BundleContext bundleContext = bundle.getBundleContext();
		httpService = getService(bundleContext, HttpService.class);
		Hi5WebResourcesAutoRegComponent resReg = getService(bundleContext, Hi5WebResourcesAutoRegComponent.class);
		String entryPoint = applicationContext.getBrandingProperty("entryPoint");
		base = "/" + entryPoint;
		alias = base + "/index.html";

		e4Runtime = new E4Runtime(base);
		e4Runtime.createE4Workbench(context);

		httpService.registerServlet(alias, new E4AppModelServlet(e4Runtime, resReg), null, null);
		resReg.registerResources(e4Runtime, base);
		context.applicationRunning();
		Application.launch(JFXApp.class);

		resReg.unregisterAll(entryPoint);

		try {
			return this.returnValue == null ? IApplication.EXIT_OK : this.returnValue;
		} finally {
			this.returnValue = null;
		}
	}

	private static <T> T getService(BundleContext bundleContext, Class<T> serviceType) {
		ServiceReference<T> ref = bundleContext.getServiceReference(serviceType);
		return bundleContext.getService(ref);
	}

	private void doStart(IApplicationContext applicationContext, JFXApp jfxApp, Stage primaryStage) {
		primaryStage.setTitle(applicationContext.getBrandingName());
		StackPane root = new StackPane();
		WebView webview = new WebView();
		webview.getEngine().load(String.format("http://localhost:8080%s", alias));
		root.getChildren().add(webview);
		primaryStage.setScene(new Scene(root, 800, 600));
		primaryStage.show();
	}

	@Override
	public final void stop() {
		httpService.unregister(alias);
	}
}
