package de.kcct.hi5.e4;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response.ResponseBuilder;

public interface AppProvider {

	ResponseBuilder respond(ContainerRequestContext ctx);
}
