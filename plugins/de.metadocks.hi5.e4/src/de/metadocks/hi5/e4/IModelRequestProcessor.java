package de.metadocks.hi5.e4;

import javax.ws.rs.container.ContainerRequestContext;

import org.eclipse.e4.ui.model.application.MApplicationElement;

public interface IModelRequestProcessor {
	MApplicationElement process(ContainerRequestContext reqCtx, MApplicationElement root);
}
