package de.metadocks.hi5.jaxrs.jersey;

import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.service.component.annotations.Component;

import de.metadocks.hi5.jaxrs.JaxRsServletFactory;

@Component(service = JaxRsServletFactory.class, immediate = true, property = "jaxrs-impl=jersey")
public class JerseyJaxRSBindingFactory implements JaxRsServletFactory {
	@Override
	public HttpServlet createServlet(Application application, Set<Object> jaxRsComponents) {
		ResourceConfig config = ResourceConfig.forApplication(application);
		config.registerInstances(jaxRsComponents.toArray());
		ServletContainer servlet = new ServletContainer(config);
		return servlet;
	}
}
