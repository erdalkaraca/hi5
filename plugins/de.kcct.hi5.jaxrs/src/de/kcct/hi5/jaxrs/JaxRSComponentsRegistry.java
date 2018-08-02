package de.kcct.hi5.jaxrs;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverterProvider;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

@Component(service = JaxRSComponentsRegistry.class, immediate = true)
public class JaxRSComponentsRegistry {
	private JaxRsServletFactory jaxRsFactory;
	private Set<Object> jaxRsComponents = new HashSet<>();

	@Reference(unbind = "-")
	public void setJaxRsFactory(JaxRsServletFactory jaxRsFactory) {
		this.jaxRsFactory = jaxRsFactory;
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public void addMessageBodyWriter(MessageBodyWriter<?> writer) {
		jaxRsComponents.add(writer);

		// TODO handle dynamic JAX-RS component registration
	}

	public void removeMessageBodyWriter(MessageBodyWriter<?> writer) {
		jaxRsComponents.remove(writer);
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public void addMessageBodyReader(MessageBodyReader<?> writer) {
		jaxRsComponents.add(writer);

		// TODO handle dynamic JAX-RS component registration
	}

	public void removeMessageBodyReader(MessageBodyReader<?> writer) {
		jaxRsComponents.remove(writer);
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public void addFeature(Feature feature) {
		jaxRsComponents.add(feature);

		// TODO handle dynamic JAX-RS component registration
	}

	public void removeFeature(Feature feature) {
		jaxRsComponents.remove(feature);
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public void addParamConverterProvider(ParamConverterProvider provider) {
		jaxRsComponents.add(provider);

		// TODO handle dynamic JAX-RS component registration
	}

	public void removeParamConverterProvider(ParamConverterProvider provider) {
		jaxRsComponents.remove(provider);
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public void addContainerRequestFilter(ContainerRequestFilter filter) {
		jaxRsComponents.add(filter);

		// TODO handle dynamic JAX-RS component registration
	}

	public void removeContainerRequestFilter(ContainerRequestFilter filter) {
		jaxRsComponents.remove(filter);
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public void addDynamicFeature(DynamicFeature feature) {
		jaxRsComponents.add(feature);

		// TODO handle dynamic JAX-RS component registration
	}

	public void removeDynamicFeature(DynamicFeature feature) {
		jaxRsComponents.remove(feature);
	}

	public String registerRestServices(HttpService httpService, String alias, HttpContext delegateHttpContext,
			Bundle bundle) throws ServletException, NamespaceException {
		ServiceReference<?>[] registeredServices = bundle.getRegisteredServices();

		if (registeredServices == null) {
			return null;
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
			return null;
		}

		String wsAlias = alias + "/ws";
		Application application = new Application() {
			public Set<Object> getSingletons() {
				return services;
			}
		};
		HttpServlet servlet = jaxRsFactory.createServlet(application, jaxRsComponents);
		httpService.registerServlet(wsAlias, servlet, null, delegateHttpContext);
		return wsAlias;
	}
}
