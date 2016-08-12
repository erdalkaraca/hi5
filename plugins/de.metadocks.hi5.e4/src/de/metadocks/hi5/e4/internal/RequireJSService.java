package de.metadocks.hi5.e4.internal;

import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.json.JSONException;
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
	public String getRequirejsConfig() {
		String script = "";
		Map<String, String> aliasToPathMap = resReg.getAliasToPathMap();

		if (!aliasToPathMap.isEmpty()) {
			JSONObject requirejsConfig = new JSONObject();

			for (Entry<String, String> e : aliasToPathMap.entrySet()) {
				try {
					requirejsConfig.put(e.getKey(), e.getValue());
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

			script = "requirejs.config({\"paths\":" + requirejsConfig.toString() + "});";
		}

		return script;
	}
}
