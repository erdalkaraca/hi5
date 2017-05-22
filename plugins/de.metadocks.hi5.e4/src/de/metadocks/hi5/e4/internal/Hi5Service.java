package de.metadocks.hi5.e4.internal;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.json.JSONException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = Hi5Service.class)
@Path("/model")
public class Hi5Service {
	private E4Runtime e4Runtime;
	private E4ModelToHTML modelTransformer;

	@Reference(unbind = "-")
	public void setModelTransformer(E4ModelToHTML modelTransformer) {
		this.modelTransformer = modelTransformer;
	}

	@Reference(unbind = "-")
	public void setE4Runtime(E4Runtime e4Runtime) {
		this.e4Runtime = e4Runtime;
	}

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/element/{id}")
	public StreamingOutput getAppElement(ContainerRequestContext reqCtx, @PathParam("id") String id) {
		MApplicationElement element = e4Runtime.process(reqCtx, e4Runtime.getModelElement(id));
		return new StreamingOutput() {

			@Override
			public void write(OutputStream out) throws IOException, WebApplicationException {
				try {
					modelTransformer.toHTML((MUIElement) element, out);
				} catch (JSONException e) {
					throw new WebApplicationException(e);
				}
			}
		};
	}
}
