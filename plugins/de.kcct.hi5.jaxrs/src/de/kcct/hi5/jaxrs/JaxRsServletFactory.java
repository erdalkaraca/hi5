package de.kcct.hi5.jaxrs;

import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.ws.rs.core.Application;

public interface JaxRsServletFactory {

	HttpServlet createServlet(Application application, Set<Object> jaxRsComponents);

}
