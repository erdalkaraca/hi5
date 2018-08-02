package de.metadocks.hi5.e4.internal;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = RequireJSService.class)
@Path("/require")
public class RequireJSService {
	private WebResourcesRegistry resReg;

	@Reference(unbind = "-")
	public void setResReg(WebResourcesRegistry resReg) {
		this.resReg = resReg;
	}

	@GET
	@Produces("application/javascript")
	@Path("/config.js")
	@PermitAll
	public String getRequirejsConfig() throws JSONException {
		JSONObject pathsConfig = resReg.getPathsConfig();
		JSONObject shimConfig = resReg.getShimConfig();
		JSONArray cssPaths = resReg.getCssPaths();
		Set<String> cssPathsSet = new LinkedHashSet<>();
		for (int i = 0; i < cssPaths.length(); i++) {
			String path = cssPaths.getString(i);
			cssPathsSet.add("\"css!" + path + "\"");
		}
		String script = String.format(";(function(){"//
				+ "require.config({"//
				+ "\"paths\": %s,"//
				+ "\"shim\": %s"//
				+ "});"//
				+ "require(%s);"//
				+ "})();", pathsConfig.toString(), shimConfig, cssPathsSet);
		return script;
	}
}
