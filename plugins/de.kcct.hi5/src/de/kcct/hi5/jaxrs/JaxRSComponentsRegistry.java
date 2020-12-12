package de.kcct.hi5.jaxrs;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
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

	public Set<String> registerRestServices(HttpService httpService, String entryPoint, String wsAlias,
			HttpContext delegateHttpContext, Bundle bundle) throws ServletException, NamespaceException {
		ServiceReference<?>[] registeredServices = bundle.getRegisteredServices();

		if (registeredServices == null) {
			registeredServices = new ServiceReference<?>[0];
		}

		BundleContext bundleContext = bundle.getBundleContext();
		Set<Object> services = new HashSet<>();

		Set<String> servicePaths = new HashSet<>();
		for (ServiceReference<?> serviceReference : registeredServices) {
			Object service = bundleContext.getService(serviceReference);

			Path pathAnnot = service.getClass().getAnnotation(Path.class);
			if (pathAnnot != null) {
				services.add(service);
				String path = pathAnnot.value();
				servicePaths.add(path);
			}
		}

		HttpServlet servlet = jaxRsFactory.createServlet(wsAlias, delegateHttpContext, services, jaxRsComponents);
		httpService.registerServlet(wsAlias, servlet, null, delegateHttpContext);
		return servicePaths;
	}
}
