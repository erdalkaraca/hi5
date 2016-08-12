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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;

@Component(service = WebResAndJaxRsComponent.class, immediate = true)
public class WebResAndJaxRsComponent {

	private LogService logService;

	private List<String> registeredResource = new ArrayList<>();
	private HttpService httpService;
	private Map<String, String> bundleNameToAlias = new HashMap<>();
	private Map<String, String> aliasToPath = new HashMap<>();

	@Reference(unbind = "-")
	public void setHttpService(HttpService httpService) {
		this.httpService = httpService;
	}

	@Reference(unbind = "-")
	public void setLogService(LogService logService) {
		this.logService = logService;
	}

	/**
	 * TODO listen to bundle changes for unregistering resources
	 * 
	 * @throws IOException
	 * @throws ConfigurationException
	 * 
	 */
	public void registerResources(String entryPoint) throws ConfigurationException {
		BundleContext bundleContext = FrameworkUtil.getBundle(WebResAndJaxRsComponent.class).getBundleContext();
		Bundle[] bundles = bundleContext.getBundles();

		for (Bundle bundle : bundles) {
			try {
				check(entryPoint, bundle);
			} catch (IOException | JSONException e) {
				logService.log(LogService.LOG_ERROR,
						"Error occurred while scanning bundle: " + bundle.getSymbolicName(), e);
				continue;
			}
		}
	}

	private void check(String entryPoint, Bundle bundle) throws IOException, JSONException {
		URL packageJson = bundle.getEntry("hi5.json");

		if (packageJson == null) {
			return;
		}

		String source = IOUtils.toString(packageJson);
		JSONObject root = new JSONObject(source);
		String webResource = root.optString("resources", "resources");
		String moduleAlias = root.optString("alias", bundle.getSymbolicName());
		String alias = moduleAlias;

		if (!alias.startsWith("/")) {
			alias = "/" + alias;
		}

		// prepend the entry point (from product definition)
		// this allows to request resources by giving a relative path
		alias = entryPoint + alias;

		if (alias.endsWith("/")) {
			alias = alias.substring(0, alias.length() - 1);
		}

		HttpContext delegateHttpContext = createHttpContext(bundle);
		try {
			httpService.registerResources(alias, webResource, delegateHttpContext);
			registeredResource.add(alias);
			bundleNameToAlias.put(bundle.getSymbolicName(), alias);

			String path = root.optString("path");

			if (!path.isEmpty()) {
				String requirejsPath = moduleAlias + "/"
						+ (path.endsWith(".js") ? path.substring(0, path.length() - 3) : path);
				aliasToPath.put(moduleAlias, requirejsPath);
			}

			registerWebservices(alias, delegateHttpContext, bundle);
		} catch (NamespaceException | ServletException e) {
			logService.log(LogService.LOG_ERROR, e.getMessage(), e);
		}
	}

	private void registerWebservices(String alias, HttpContext delegateHttpContext, Bundle bundle)
			throws ServletException, NamespaceException {
		ServiceReference<?>[] registeredServices = bundle.getRegisteredServices();

		if (registeredServices == null) {
			return;
		}

		BundleContext bundleContext = bundle.getBundleContext();
		Set<Object> services = new HashSet<>();

		for (ServiceReference<?> serviceReference : registeredServices) {
			Object service = bundleContext.getService(serviceReference);

			if (service.getClass().isAnnotationPresent(Path.class)
					|| service.getClass().isAnnotationPresent(Produces.class)) {
				services.add(service);
			}
		}

		if (services.isEmpty()) {
			return;
		}

		String wsAlias = alias + "/ws";
		Application application = new Application() {
			public Set<Object> getSingletons() {
				return services;
			};
		};
		ServletContainer servletContainer = new ServletContainer(ResourceConfig.forApplication(application));
		httpService.registerServlet(wsAlias, servletContainer, null, delegateHttpContext);
	}

	public void unregisterAll(String entryPoint) {
		Predicate<? super String> predicate = res -> res.startsWith(entryPoint);

		registeredResource.stream().filter(predicate).forEach(alias -> {
			httpService.unregister(alias);
		});

		registeredResource.stream().filter(predicate).forEach(alias -> {
			registeredResource.remove(alias);
		});

		bundleNameToAlias.keySet().stream().filter(predicate).forEach(alias -> {
			bundleNameToAlias.remove(alias);
		});
	}

	public static HttpContext createHttpContext(Bundle bundle) {
		HttpContext delegate = new HttpContext() {

			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return true;
			}

			@Override
			public URL getResource(String name) {
				return bundle.getResource(name);
			}

			@Override
			public String getMimeType(String name) {
				return null;
			}
		};
		return delegate;
	}

	public String getBundleAliasMapping(String bundleSymbolicName) {
		return bundleNameToAlias.get(bundleSymbolicName);
	}

	public Map<String, String> getAliasToPathMap() {
		return Collections.unmodifiableMap(aliasToPath);
	}
}
