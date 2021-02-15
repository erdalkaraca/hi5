package de.kcct.hi5.jaxrs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.json.JSONException;
import org.json.JSONObject;

public interface JSApiProvider {

	public static final String APPLICATION_JAVASCRIPT = "application/javascript";
	public static final String API_JS = "api.js";

	@GET
	@Path(API_JS)
	@Produces(APPLICATION_JAVASCRIPT)
	// this method is considered static content, hence allowed to be called by
	// unauthenticated requests
	@PermitAll
	default String jsApi(@Context UriInfo uriInfo) throws JSONException {
		Class<?> jxComponentType = getClass();
		return createJSStubs(uriInfo, jxComponentType);
	}

	static String createJSStubs(UriInfo uriInfo, Class<?> jxComponentType) throws JSONException {
		List<PathSegment> pathSegments = new ArrayList<>(uriInfo.getPathSegments());
		pathSegments.remove(pathSegments.size() - 1);
		String url = uriInfo.getBaseUri().toString()
				+ pathSegments.stream().map(s -> s.getPath()).collect(Collectors.joining("/"));
		try {
			// take only the path of the URL into account as the base
			// (host:port) is not needed and makes reverse proxies (nginx to
			// embedded jetty, for example) not work properly
			url = new URL(url).getPath();
		} catch (MalformedURLException e) {
			Logger.getLogger(jxComponentType.getName()).log(Level.SEVERE, e.getMessage(), e);
		}

		StringBuilder sb = new StringBuilder();
		Method[] declaredMethods = jxComponentType.getDeclaredMethods();

		for (Method method : declaredMethods) {
			Path path = method.getAnnotation(Path.class);
			if (path == null) {
				continue;
			}

			Produces produces = method.getAnnotation(Produces.class);
			if (produces == null) {
				produces = method.getDeclaringClass().getAnnotation(Produces.class);
			}

			Consumes consumes = method.getAnnotation(Consumes.class);
			if (consumes == null) {
				consumes = method.getDeclaringClass().getAnnotation(Consumes.class);
			}

			Class<? extends Annotation> httpMethod = GET.class;
			List<Class<? extends Annotation>> asList = Arrays.asList(POST.class, PUT.class, DELETE.class);
			for (Class<? extends Annotation> methodType : asList) {
				if (method.isAnnotationPresent(methodType)) {
					httpMethod = methodType;
					break;
				}
			}

			JSONObject settings = new JSONObject();
			String methodUrl = path.value();
			settings.put("url", methodUrl);
			settings.put("method", httpMethod.getSimpleName());

			if (produces != null) {
				String value = produces.value()[0];
				String dataType = null;
				if (value.endsWith("xml")) {
					dataType = "xml";
				} else if (MediaType.TEXT_PLAIN.equals(value)) {
					dataType = "text";
				} else if (MediaType.TEXT_HTML.equals(value)) {
					dataType = "html";
				} else if (MediaType.APPLICATION_JSON.equals(value)) {
					dataType = "json";
				}
				if (dataType != null) {
					settings.put("dataType", dataType);
				}
			}

			if (consumes != null && httpMethod != GET.class) {
				settings.put("contentType", consumes.value()[0]);
			}

			// map each settings object to a javascript method stub by
			// delegating to the aj function
			sb.append(String.format("%s:o=>{io.ax(u,%s,o);},\n", method.getName(), settings.toString()));
		}

		
		// transform the "params: {key='value'}" object into a query
		// param string if available
		String moduleDefinition = String.format("define(['hi5-io'],function(io){\n"//
				+ "var u='" + url + "';\n"//
				+ "return {\n%s}});", sb.toString());
		return moduleDefinition;
	}
}
