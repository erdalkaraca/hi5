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
package de.kcct.hi5.internal;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import de.kcct.hi5.jaxrs.JaxRSComponentsRegistry;

@Component(service = WebResourcesRegistry.class, immediate = true)
public class WebResourcesRegistry {

	private static final Logger LOG = Logger.getLogger(WebResourcesRegistry.class.getName());
	private HttpService httpService;
	private JaxRSComponentsRegistry jaxRsComponentsRegistry;

	private List<String> registeredResources = new ArrayList<>();
	private Map<String, String> bundleNameToAlias = new HashMap<>();
	private JSONObject pathsConfig = new JSONObject();
	private JSONObject shim = new JSONObject();
	private JSONArray cssPaths = new JSONArray();

	@Reference(unbind = "-")
	public void setHttpService(HttpService httpService) {
		this.httpService = httpService;
	}

	@Reference(unbind = "-")
	public void setJaxRsComponentsRegistry(JaxRSComponentsRegistry jaxRsComponentsRegistry) {
		this.jaxRsComponentsRegistry = jaxRsComponentsRegistry;
	}

	/**
	 * TODO listen to bundle changes for unregistering resources
	 * 
	 * @throws IOException
	 * @throws ConfigurationException
	 * 
	 */
	public void registerResources(String entryPoint) throws ConfigurationException {
		BundleContext bundleContext = FrameworkUtil.getBundle(WebResourcesRegistry.class).getBundleContext();
		Bundle[] bundles = bundleContext.getBundles();

		for (Bundle bundle : bundles) {
			try {
				check(entryPoint, bundle);
			} catch (IOException | JSONException e) {
				LOG.log(Level.SEVERE, "Error occurred while scanning bundle: " + bundle.getSymbolicName(), e);
				continue;
			}
		}
	}

	private void check(String entryPoint, Bundle bundle) throws IOException, JSONException {
		String moduleAlias = null;
		JSONObject root = null;

		URL packageJson = bundle.getEntry("hi5.json");
		if (packageJson != null) {
			String source = IOUtils.toString(packageJson);
			root = new JSONObject(source);
			moduleAlias = root.optString("alias", null);
		}

		if (moduleAlias == null) {
			String aliasHeader = bundle.getHeaders().get("Hi5-Alias");

			if (aliasHeader != null) {
				aliasHeader = aliasHeader.trim();
				moduleAlias = aliasHeader.isEmpty() ? bundle.getSymbolicName() : aliasHeader;
				root = new JSONObject();
			}
		}
		
		if (moduleAlias == null) {
			return;
		}

		collectModuleResources(entryPoint, bundle, root, moduleAlias);
		collectModuleMetadata(root, moduleAlias);
	}

	private void collectModuleResources(String entryPoint, Bundle bundle, JSONObject root, String moduleAlias)
			throws JSONException {
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

		try {
			String webResource = root.optString("resources", "resources");
			HttpContext delegateHttpContext = createHttpContext(bundle, webResource);
			registeredResources.add(alias);
			bundleNameToAlias.put(bundle.getSymbolicName(), alias);
			String wsAlias = alias;
			Set<String> servicePaths = jaxRsComponentsRegistry.registerRestServices(httpService, entryPoint, wsAlias,
					delegateHttpContext, bundle);
			LOG.fine("registered REST services: " + servicePaths);
		} catch (NamespaceException | ServletException e) {
			LOG.log(Level.SEVERE, "Collecting module resources failed for module " + bundle.getSymbolicName(), e);
		}
	}

	@SuppressWarnings("rawtypes")
	private void collectModuleMetadata(JSONObject root, String moduleAlias) throws JSONException {
		// register modules
		{
			JSONObject modules = root.optJSONObject("modules");

			if (modules != null) {
				Iterator keys = modules.keys();

				while (keys.hasNext()) {
					String key = (String) keys.next();
					String path = modules.getString(key);
					String ma = moduleAlias;

					if (!ma.endsWith("/")) {
						ma += "/";
					}

					if ("/".equals(ma)) {
						ma = "";
					}

					String requirejsPath = ma + trimJSExtension(path);
					pathsConfig.put(key, requirejsPath);
				}
			}
		}

		// collect bundle shim and merge with main shim object
		{
			JSONObject bundleShim = root.optJSONObject("shim");

			if (bundleShim != null) {
				Iterator keys = bundleShim.keys();

				while (keys.hasNext()) {
					String key = (String) keys.next();
					Object object = bundleShim.get(key);
					shim.put(key, object);
				}
			}
		}

		// collect css definitions
		{
			JSONArray css = root.optJSONArray("css");
			if (css != null) {
				for (int i = 0; i < css.length(); i++) {
					String cssPath = css.getString(i);
					if (!moduleAlias.startsWith("/")) {
						// prepend module alias except the app module which has no alias
						cssPath = moduleAlias + "/" + cssPath;
					}
					cssPaths.put(cssPath);
				}
			}
		}
	}

	private String trimJSExtension(String path) {
		return path.endsWith(".js") ? path.substring(0, path.length() - 3) : path;
	}

	public void unregisterAll(String entryPoint) {
		Predicate<? super String> predicate = res -> res.startsWith(entryPoint);

		registeredResources.stream().filter(predicate).forEach(alias -> {
			httpService.unregister(alias);
			registeredResources.remove(alias);
		});

		bundleNameToAlias.keySet().stream().filter(predicate).forEach(alias -> {
			bundleNameToAlias.remove(alias);
		});
	}

	public static HttpContext createHttpContext(Bundle bundle, String webResource) {
		HttpContext delegate = new HttpContext() {

			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return true;
			}

			@Override
			public URL getResource(String name) {
				String fullPath = webResource + "/" + name;
				return bundle.getResource(fullPath);
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

	public JSONObject getPathsConfig() {
		return pathsConfig;
	}

	public JSONObject getShimConfig() {
		return shim;
	}

	public JSONArray getCssPaths() {
		return cssPaths;
	}
}
