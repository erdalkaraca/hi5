package de.kcct.hi5.internal;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import de.kcct.hi5.AppProvider;

@Component(immediate = true, service = AppsService.class)
@Path("/")
public class AppsService {

	private Map<String, AppProvider> appProviders = new HashMap<>();

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public void addAppProvider(AppProvider provider, Map<String, Object> props) {
		String name = (String) props.get("name");
		appProviders.put(name, provider);
	}

	public void removeAppProvider(AppProvider provider) {
		appProviders.entrySet().stream().filter(e -> e.getValue() == provider).forEach(e -> {
			appProviders.remove(e.getKey());
		});
	}

	@GET
	@Path("/app-{id}")
	@PermitAll
	public Response getPage(ContainerRequestContext ctx, @PathParam("id") String id) {
		AppProvider appProvider = appProviders.get(id);
		if (appProvider == null) {
			throw new NotFoundException("App not found: " + id);
		}

		return appProvider.respond(ctx).build();
	}
}
