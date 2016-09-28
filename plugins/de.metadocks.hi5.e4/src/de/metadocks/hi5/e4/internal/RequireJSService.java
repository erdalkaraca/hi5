package de.metadocks.hi5.e4.internal;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = RequireJSService.class)
@Path("/require")
public class RequireJSService {
	private WebResAndJaxRsComponent resReg;

	@Reference(unbind = "-")
	public void setResReg(WebResAndJaxRsComponent resReg) {
		this.resReg = resReg;
	}

	@GET
	@Produces("application/javascript")
	@Path("/config.js")
	@PermitAll
	public String getRequirejsConfig() {
		String script = "";
		JSONObject pathsConfig = resReg.getPathsConfig();
		JSONObject shimConfig = resReg.getShimConfig();
		script = "requirejs.config({\"paths\":" + pathsConfig.toString() + ",\"shim\":" + shimConfig + "});";
		return script;
	}
}
