package de.kcct.hi5.jaxrs;

import java.util.Set;

import javax.servlet.http.HttpServlet;

import org.osgi.service.http.HttpContext;

public interface JaxRsServletFactory {

	HttpServlet createServlet(String alias, HttpContext httpContext, Set<Object> applicationSingletonServices,
			Set<Object> jaxRsComponents);

}
