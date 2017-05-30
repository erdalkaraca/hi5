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
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.contributions.IContributionFactory;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.internal.workbench.E4Workbench;
import org.eclipse.e4.ui.internal.workbench.ModelServiceImpl;
import org.eclipse.e4.ui.internal.workbench.PlaceholderResolver;
import org.eclipse.e4.ui.internal.workbench.ReflectionContributionFactory;
import org.eclipse.e4.ui.internal.workbench.ResourceHandler;
import org.eclipse.e4.ui.internal.workbench.SelectionAggregator;
import org.eclipse.e4.ui.internal.workbench.URIHelper;
import org.eclipse.e4.ui.internal.workbench.WorkbenchLogger;
import org.eclipse.e4.ui.model.application.MAddon;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.basic.impl.BasicPackageImpl;
import org.eclipse.e4.ui.model.application.ui.impl.UiPackageImpl;
import org.eclipse.e4.ui.workbench.IModelResourceHandler;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.e4.ui.workbench.lifecycle.ProcessAdditions;
import org.eclipse.e4.ui.workbench.lifecycle.ProcessRemovals;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPlaceholderResolver;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.util.tracker.ServiceTracker;

import de.metadocks.hi5.e4.IModelRequestProcessor;

@SuppressWarnings("restriction")
@Component(service = E4Runtime.class)
public class E4Runtime {
	private static final Logger LOG = Logger.getLogger(E4Runtime.class.getName());
	private static final String CONTEXT_INITIALIZED = "org.eclipse.ui.contextInitialized";

	private E4Workbench workbench;
	private String[] args;
	private Object lcManager;
	private ServiceTracker<?, Location> locationTracker;
	private IModelResourceHandler handler;
	private IApplicationContext applicationContext;
	private IModelRequestProcessor modelRequestProcessor;
	private Map<String, MApplicationElement> index = new HashMap<String, MApplicationElement>();

	public MApplication copyApplicationModel() {
		if (workbench == null) {
			throw new IllegalStateException("E4 runtime not initialized.");
		}

		MApplication application = workbench.getApplication();
		MApplication copy = (MApplication) EcoreUtil.copy((EObject) application);
		return (MApplication) copy;
	}

	public E4Workbench createE4Workbench(IApplicationContext applicationContext) {
		this.applicationContext = applicationContext;

		String modelRequestProcessorValue = applicationContext.getBrandingProperty("modelRequestProcessor");
		if (modelRequestProcessorValue != null) {
			BundleContext bundleContext = applicationContext.getBrandingBundle().getBundleContext();
			try {
				Collection<ServiceReference<IModelRequestProcessor>> serviceReferences = bundleContext
						.getServiceReferences(IModelRequestProcessor.class,
								"(component.name=" + modelRequestProcessorValue + ")");
				if (serviceReferences != null && !serviceReferences.isEmpty()) {
					ServiceReference<IModelRequestProcessor> next = serviceReferences.iterator().next();
					modelRequestProcessor = bundleContext.getService(next);
				}
			} catch (InvalidSyntaxException e) {
				LOG.log(Level.SEVERE, "Could not instantiate model request processor: " + modelRequestProcessorValue,
						e);
			}

			if (modelRequestProcessor == null) {
				LOG.log(Level.SEVERE, "Could not find model request processor: " + modelRequestProcessorValue);
			}
		}

		args = (String[]) applicationContext.getArguments().get(IApplicationContext.APPLICATION_ARGS);

		IEclipseContext appContext = createDefaultContext();
		appContext.set(IApplicationContext.class, applicationContext);

		// This context will be used by the injector for its
		// extended data suppliers
		ContextInjectionFactory.setDefault(appContext);

		// Get the factory to create DI instances with
		IContributionFactory factory = appContext.get(IContributionFactory.class);

		// Install the life-cycle manager for this session if there's one
		// defined
		Optional<String> lifeCycleURI = getArgValue(IWorkbench.LIFE_CYCLE_URI_ARG, applicationContext, false);
		lifeCycleURI.ifPresent(lifeCycleURIValue -> {
			lcManager = factory.create(lifeCycleURIValue, appContext);
			if (lcManager != null) {
				// Let the manager manipulate the appContext if desired
				ContextInjectionFactory.invoke(lcManager, PostContextCreate.class, appContext, null);
			}
		});

		// Create the app model and its context
		MApplication appModel = loadApplicationModel(applicationContext, appContext);
		appModel.setContext(appContext);

		appModel.getTransientData().put(E4Workbench.RTL_MODE, false);

		// for compatibility layer: set the application in the OSGi service
		// context (see Workbench#getInstance())
		if (!E4Workbench.getServiceContext().containsKey(MApplication.class)) {
			// first one wins.
			E4Workbench.getServiceContext().set(MApplication.class, appModel);
		}

		// Set the app's context after adding itself
		appContext.set(MApplication.class, appModel);

		// adds basic services to the contexts
		initializeServices(appModel);

		// let the life cycle manager add to the model
		if (lcManager != null) {
			ContextInjectionFactory.invoke(lcManager, ProcessAdditions.class, appContext, null);
			ContextInjectionFactory.invoke(lcManager, ProcessRemovals.class, appContext, null);
		}

		// Create the addons
		IEclipseContext addonStaticContext = EclipseContextFactory.create();
		for (MAddon addon : appModel.getAddons()) {
			addonStaticContext.set(MAddon.class, addon);
			Object obj = factory.create(addon.getContributionURI(), appContext, addonStaticContext);
			addon.setObject(obj);
		}

		// Parse out parameters from both the command line and/or the product
		// definition (if any) and put them in the context
		Optional<String> xmiURI = getArgValue(IWorkbench.XMI_URI_ARG, applicationContext, false);
		xmiURI.ifPresent(xmiURIValue -> {
			appContext.set(IWorkbench.XMI_URI_ARG, xmiURIValue);
		});

		Optional<String> rendererFactoryURI = getArgValue(E4Workbench.RENDERER_FACTORY_URI, applicationContext, false);
		rendererFactoryURI.ifPresent(rendererFactoryURIValue -> {
			appContext.set(E4Workbench.RENDERER_FACTORY_URI, rendererFactoryURIValue);
		});

		// Instantiate the Workbench (which is responsible for
		// 'running' the UI (if any)...
		appContext.set(UISynchronize.class, new UISynchronize() {

			@Override
			public void syncExec(Runnable runnable) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void asyncExec(Runnable runnable) {
				throw new UnsupportedOperationException();
			}
		});
		workbench = new E4Workbench(appModel, appContext);

		indexElements();
		return workbench;
	}

	private void indexElements() {
		TreeIterator<EObject> iter = ((EObject) workbench.getApplication()).eAllContents();
		while (iter.hasNext()) {
			EObject eObject = (EObject) iter.next();

			if (eObject instanceof MApplicationElement) {
				MApplicationElement appElement = (MApplicationElement) eObject;
				String elementId = appElement.getElementId();
				MApplicationElement replaced = index.put(elementId, appElement);
				if (replaced != null) {
					LOG.severe("An existing element was replaced: " + elementId + " -> " + replaced);
				}
			}
		}
	}

	public static IEclipseContext createDefaultContext() {
		IEclipseContext serviceContext = createDefaultHeadlessContext();
		final IEclipseContext appContext = serviceContext.createChild("WorkbenchContext"); //$NON-NLS-1$
		// make application context available for dependency injection under the
		// E4Application.APPLICATION_CONTEXT_KEY key
		appContext.set(IWorkbench.APPLICATION_CONTEXT_KEY, appContext);

		appContext.set("org.eclipse.e4.core.services.log.Logger",
				ContextInjectionFactory.make(WorkbenchLogger.class, appContext));
		appContext.set(EModelService.class, new ModelServiceImpl(appContext));
		appContext.set(EPlaceholderResolver.class, new PlaceholderResolver());

		return appContext;
	}

	public static IEclipseContext createDefaultHeadlessContext() {
		IEclipseContext serviceContext = EclipseContextFactory.getServiceContext(getBundleContext());
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		serviceContext.set(IExtensionRegistry.class, registry);
		ReflectionContributionFactory contributionFactory = new ReflectionContributionFactory(registry);
		serviceContext.set(IContributionFactory.class, contributionFactory);
		return serviceContext;
	}

	private static BundleContext getBundleContext() {
		return FrameworkUtil.getBundle(E4Runtime.class).getBundleContext();
	}

	/**
	 * Finds an argument's value in the app's command line arguments, branding,
	 * and system properties
	 *
	 * @param argName
	 *            the argument name
	 * @param appContext
	 *            the application context
	 * @param singledCmdArgValue
	 *            whether it's a single-valued argument
	 * @return an {@link Optional} containing the value or an empty
	 *         {@link Optional}, if no value could be found
	 */
	private Optional<String> getArgValue(String argName, IApplicationContext appContext, boolean singledCmdArgValue) {
		// Is it in the arg list ?
		if (argName == null || argName.length() == 0)
			return Optional.empty();

		if (singledCmdArgValue) {
			for (int i = 0; i < args.length; i++) {
				if (("-" + argName).equals(args[i]))
					return Optional.of("true");
			}
		} else {
			for (int i = 0; i < args.length; i++) {
				if (("-" + argName).equals(args[i]) && i + 1 < args.length)
					return Optional.of(args[i + 1]);
			}
		}

		final String brandingProperty = appContext.getBrandingProperty(argName);

		return Optional.ofNullable(brandingProperty).map(brandingPropertyValue -> Optional.of(brandingPropertyValue))
				.orElse(Optional.ofNullable(System.getProperty(argName)));
	}

	private MApplication loadApplicationModel(IApplicationContext appContext, IEclipseContext eclipseContext) {
		MApplication theApp = null;

		Location instanceLocation = getInstanceLocation();

		URI applicationModelURI = determineApplicationModelURI(appContext);
		eclipseContext.set(E4Workbench.INITIAL_WORKBENCH_MODEL_URI, applicationModelURI);

		// Save and restore
		Boolean saveAndRestore = getArgValue(IWorkbench.PERSIST_STATE, appContext, false)
				.map(value -> Boolean.parseBoolean(value)).orElse(Boolean.TRUE);

		eclipseContext.set(IWorkbench.PERSIST_STATE, saveAndRestore);

		// when -data @none or -data @noDefault options
		if (instanceLocation != null && instanceLocation.getURL() != null) {
			eclipseContext.set(E4Workbench.INSTANCE_LOCATION, instanceLocation);
		} else {
			eclipseContext.set(IWorkbench.PERSIST_STATE, false);
		}

		// Persisted state
		Boolean clearPersistedState = getArgValue(IWorkbench.CLEAR_PERSISTED_STATE, appContext, true)
				.map(value -> Boolean.parseBoolean(value)).orElse(Boolean.FALSE);
		eclipseContext.set(IWorkbench.CLEAR_PERSISTED_STATE, clearPersistedState);

		String resourceHandler = getArgValue(IWorkbench.MODEL_RESOURCE_HANDLER, appContext, false)
				.orElse("bundleclass://org.eclipse.e4.ui.workbench/" + ResourceHandler.class.getName());

		IContributionFactory factory = eclipseContext.get(IContributionFactory.class);

		handler = (IModelResourceHandler) factory.create(resourceHandler, eclipseContext);
		eclipseContext.set(IModelResourceHandler.class, handler);

		Resource resource = handler.loadMostRecentModel();
		theApp = (MApplication) resource.getContents().get(0);

		return theApp;
	}

	private URI determineApplicationModelURI(IApplicationContext appContext) {
		Optional<String> appModelPath = getArgValue(IWorkbench.XMI_URI_ARG, appContext, false);

		String appModelPathValue = appModelPath.filter(path -> !path.isEmpty()).orElseGet(() -> {
			Bundle brandingBundle = appContext.getBrandingBundle();
			if (brandingBundle != null) {
				return brandingBundle.getSymbolicName() + "/" + "Application.e4xmi";
			} else {
				WorkbenchLogger logger = new WorkbenchLogger(getBundleContext().getBundle().getSymbolicName());
				logger.error(new Exception(), "applicationXMI parameter not set and no branding plugin defined. "); //$NON-NLS-1$
			}
			return null;
		});

		URI applicationModelURI = null;

		// check if the appModelPath is already a platform-URI and if so use it
		if (URIHelper.isPlatformURI(appModelPathValue)) {
			applicationModelURI = URI.createURI(appModelPathValue, true);
		} else {
			applicationModelURI = URI.createPlatformPluginURI(appModelPathValue, true);
		}
		return applicationModelURI;

	}

	/**
	 * @return the instance Location service
	 */
	public Location getInstanceLocation() {
		if (locationTracker == null) {
			Filter filter = null;
			try {
				filter = getBundleContext().createFilter(Location.INSTANCE_FILTER);
			} catch (InvalidSyntaxException e) {
				// ignore this. It should never happen as we have tested the
				// above format.
			}
			locationTracker = new ServiceTracker<>(getBundleContext(), filter, null);
			locationTracker.open();
		}
		return locationTracker.getService();
	}

	static public void initializeServices(MApplication appModel) {
		IEclipseContext appContext = appModel.getContext();
		// make sure we only add trackers once
		if (appContext.containsKey(CONTEXT_INITIALIZED))
			return;
		appContext.set(CONTEXT_INITIALIZED, "true");
		// initializeApplicationServices(appContext);
		List<MWindow> windows = appModel.getChildren();
		for (MWindow childWindow : windows) {
			initializeWindowServices(childWindow);
		}
		((EObject) appModel).eAdapters().add(new AdapterImpl() {
			@Override
			public void notifyChanged(Notification notification) {
				if (notification.getFeatureID(MApplication.class) != UiPackageImpl.ELEMENT_CONTAINER__CHILDREN)
					return;
				if (notification.getEventType() != Notification.ADD)
					return;
				MWindow childWindow = (MWindow) notification.getNewValue();
				initializeWindowServices(childWindow);
			}
		});
	}

	private static void initializeWindowServices(MWindow childWindow) {
		IEclipseContext windowContext = childWindow.getContext();
		initWindowContext(windowContext);
		// Mostly MWindow contexts are lazily created by renderers and is not
		// set at this point.
		((EObject) childWindow).eAdapters().add(new AdapterImpl() {
			@Override
			public void notifyChanged(Notification notification) {
				if (notification.getFeatureID(MWindow.class) != BasicPackageImpl.WINDOW__CONTEXT)
					return;
				IEclipseContext windowContext = (IEclipseContext) notification.getNewValue();
				initWindowContext(windowContext);
			}
		});
	}

	static private void initWindowContext(IEclipseContext windowContext) {
		if (windowContext == null)
			return;
		SelectionAggregator selectionAggregator = ContextInjectionFactory.make(SelectionAggregator.class,
				windowContext);
		windowContext.set(SelectionAggregator.class, selectionAggregator);
	}

	public InputStream getIndexFile() throws IOException {
		String indexPath = applicationContext.getBrandingProperty("index");

		if (indexPath == null) {
			indexPath = "resources/index.html";
		}

		URL entry = applicationContext.getBrandingBundle().getEntry(indexPath);
		return entry.openStream();
	}

	public MApplicationElement getModelElement(String id) {
		return index.get(id);
	}

	public MApplicationElement process(ContainerRequestContext reqCtx, MApplicationElement element) {
		if (modelRequestProcessor != null) {
			return modelRequestProcessor.process(reqCtx, element);
		}
		return element;
	}
}
