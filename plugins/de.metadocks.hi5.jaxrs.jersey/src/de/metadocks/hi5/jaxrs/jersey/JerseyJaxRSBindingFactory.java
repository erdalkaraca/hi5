package de.metadocks.hi5.jaxrs.jersey;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.ServiceLocatorProvider;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import de.metadocks.hi5.jaxrs.JaxRsServletFactory;

@Component(service = JaxRsServletFactory.class, immediate = true, property = "jaxrs-impl=jersey")
public class JerseyJaxRSBindingFactory implements JaxRsServletFactory {
	private FactoryExt<?> factory;

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public void addDIProvider(FactoryExt<?> factory) {
		this.factory = factory;
	}

	public void removeDIProvider(FactoryExt<?> factory) {
	}

	@Override
	public HttpServlet createServlet(Application application, Set<Object> jaxRsComponents) {
		ResourceConfig config = ResourceConfig.forApplication(application);
		config.registerInstances(jaxRsComponents.toArray());
		config.register(new Feature() {

			@Override
			public boolean configure(FeatureContext ctx) {
				ctx.register(new AbstractBinder() {
					@Override
					protected void configure() {
						ServiceLocator serviceLocator = ServiceLocatorProvider.getServiceLocator(ctx);
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
					}
				});
				return false;
			}
		});

		ServletContainer servlet = new ServletContainer(config);
		return servlet;
	}
}
