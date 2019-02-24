package de.kcct.hi5.jaxrs.jersey;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.servlet.ServletContainer;
import org.json.JSONException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.HttpContext;

import de.kcct.hi5.jaxrs.JSApiProvider;
import de.kcct.hi5.jaxrs.JaxRsServletFactory;

@Component(service = JaxRsServletFactory.class, immediate = true, property = "jaxrs-impl=jersey")
public class JerseyJaxRSBindingFactory implements JaxRsServletFactory {
	@Override
	public HttpServlet createServlet(String alias, HttpContext httpContext, Set<Object> applicationSingletonServices,
			Set<Object> jaxRsComponents) {
		Application application = new Application() {
			public Set<Object> getSingletons() {
				return applicationSingletonServices;
			}
		};
		ResourceConfig config = ResourceConfig.forApplication(application);
		config.registerInstances(jaxRsComponents.toArray());

		List<Resource> resources = new ArrayList<>();
		{
			Resource.Builder resourceBuilder = Resource.builder("/");
			Inflector<ContainerRequestContext, ?> inflector = new Inflector<ContainerRequestContext, Object>() {

				@Override
				@PermitAll
				public Object apply(ContainerRequestContext data) {
					String path = data.getUriInfo().getPath();
					URL resource = httpContext.getResource(path);
					try {
						return resource.openStream();
					} catch (Exception e) {
//						throw new NotFoundException();
						return null;
					}
				}
			};
			resourceBuilder.addChildResource("{path: .*}").addMethod(GET.class.getSimpleName()).handledBy(inflector);
			Resource resource = resourceBuilder.build();
			resources.add(resource);
		}

		// add a child resource whose path ends with ../api.js
		{
			List<Resource> apiResources = config.getSingletons().stream()//
					.filter(singleton -> !(singleton instanceof JSApiProvider)
							&& singleton.getClass().isAnnotationPresent(javax.ws.rs.Path.class))//
					.map(singleton -> {
						Path path = singleton.getClass().getAnnotation(javax.ws.rs.Path.class);
						Resource.Builder resourceBuilder = Resource.builder(path.value());
						resourceBuilder.addChildResource(JSApiProvider.API_JS)//
								.addMethod(GET.class.getSimpleName())//
								.produces(JSApiProvider.APPLICATION_JAVASCRIPT)//
								.handledBy(new Inflector<ContainerRequestContext, String>() {

									@Override
									@PermitAll
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
					.collect(Collectors.toList());
			resources.addAll(apiResources);
		}

		config.registerResources(resources.toArray(new Resource[0]));

		ServletContainer servlet = new ServletContainer(config);
		return servlet;
	}
}
