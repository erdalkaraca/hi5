package de.kcct.hi5.jaxrs.jersey;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServlet;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.ServiceLocatorProvider;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.servlet.ServletContainer;
import org.json.JSONException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import de.kcct.hi5.jaxrs.JSApiProvider;
import de.kcct.hi5.jaxrs.JaxRsServletFactory;

@Component(service = JaxRsServletFactory.class, immediate = true, property = "jaxrs-impl=jersey")
public class JerseyJaxRSBindingFactory implements JaxRsServletFactory {
	private Set<FactoryExt<?>> factories = new HashSet<>();

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public void addDIProvider(FactoryExt<?> factory) {
		factories.add(factory);
	}

	public void removeDIProvider(FactoryExt<?> factory) {
		factories.remove(factory);
	}

	@Override
	public HttpServlet createServlet(Application application, Set<Object> jaxRsComponents) {
		ResourceConfig config = ResourceConfig.forApplication(application);
		config.registerInstances(jaxRsComponents.toArray());

		// add a child resource whose path ends with ../api.js
		Set<Resource> apiResources = config.getSingletons().stream()//
				.filter(singleton -> !(singleton instanceof JSApiProvider)
						&& singleton.getClass().isAnnotationPresent(javax.ws.rs.Path.class))//
				.map(singleton -> {
					Path path = singleton.getClass().getAnnotation(javax.ws.rs.Path.class);
					Resource.Builder resourceBuilder = Resource.builder(path.value());
					resourceBuilder.addChildResource(JSApiProvider.API_JS).addMethod(GET.class.getSimpleName())
							.produces(JSApiProvider.APPLICATION_JAVASCRIPT)
							.handledBy(new Inflector<ContainerRequestContext, String>() {

								@Override
								public String apply(ContainerRequestContext data) {
									try {
										return JSApiProvider.createJSStubs(data.getUriInfo(), singleton.getClass());
									} catch (JSONException e) {
										throw new WebApplicationException(e);
									}
								}
							});
					return resourceBuilder.build();
				})//
				.collect(Collectors.toSet());
		if (!apiResources.isEmpty()) {
			config.registerResources(apiResources);
		}

		// TODO dynamic behavior of services
		if (!factories.isEmpty()) {
			config.register(new Feature() {

				@Override
				public boolean configure(FeatureContext ctx) {
					ctx.register(new AbstractBinder() {
						@Override
						protected void configure() {
							ServiceLocator serviceLocator = ServiceLocatorProvider.getServiceLocator(ctx);
							factories.forEach(factory -> {
								bindFactory(new Factory<Object>() {
									@Override
									public void dispose(Object arg0) {
									}

									@Override
									public Object provide() {
										serviceLocator.inject(factory);
										return factory.provide();
									}
								}).to(factory.getType()).in(RequestScoped.class);
							});
						}
					});
					return false;
				}
			});
		}

		ServletContainer servlet = new ServletContainer(config);
		return servlet;
	}
}
