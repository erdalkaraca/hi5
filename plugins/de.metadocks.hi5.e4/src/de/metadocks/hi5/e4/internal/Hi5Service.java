package de.metadocks.hi5.e4.internal;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.json.JSONException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = Hi5Service.class)
@Path("/app")
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
	public String getAppElement(@PathParam("id") String id) {
		MUIElement element = e4Runtime.getModelElement(id);
		String html = "";
		try {
			html = modelTransformer.toHTML(element);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return html;
	}
}
