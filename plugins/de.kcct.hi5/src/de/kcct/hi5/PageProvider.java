package de.kcct.hi5;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response.ResponseBuilder;

public interface PageProvider {

	ResponseBuilder respond(ContainerRequestContext ctx);
}
