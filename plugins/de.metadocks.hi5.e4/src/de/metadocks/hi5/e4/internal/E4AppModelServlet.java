package de.metadocks.hi5.e4.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledToolItem;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarElement;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.FrameworkUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

@SuppressWarnings("unchecked")
public class E4AppModelServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private E4Runtime e4Runtime;
	private Hi5WebResourcesAutoRegComponent resReg;
	private E4ModelToHTML modelTransformer = new E4ModelToHTML();

	public E4AppModelServlet(E4Runtime e4Runtime, Hi5WebResourcesAutoRegComponent resReg) {
		this.e4Runtime = e4Runtime;
		this.resReg = resReg;
	}

	private DocumentBuilder builder;
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(false);
		dbf.setNamespaceAware(true);
		dbf.setIgnoringComments(false);
		dbf.setIgnoringElementContentWhitespace(false);
		dbf.setExpandEntityReferences(false);

		try {
			builder = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Transformer transformer;
	{
		TransformerFactory tf = TransformerFactory.newInstance();
		try {
			transformer = tf.newTransformer();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		MApplication appModel = e4Runtime.copyApplicationModel();
		resp.setContentType("text/html");

		InputStream index = e4Runtime.getIndexFile();
		Document doc = null;

		try {
			doc = builder.parse(index);
			toHTML(doc, appModel);
		} catch (JSONException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		PrintWriter writer = resp.getWriter();

		try {
			transformer.transform(new DOMSource(doc), new StreamResult(writer));
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		resp.flushBuffer();
	}

	private void toHTML(Document doc, MApplication appModel) throws JSONException {
		Element head = (Element) doc.getElementsByTagName("head").item(0);
		Element body = (Element) doc.getElementsByTagName("body").item(0);

		// it is important to provide the RequireJS library
		createScript(head, "requirejs/require.js");

		// setup RequireJS modules
		{
			Map<String, String> aliasToPathMap = resReg.getAliasToPathMap();

			if (!aliasToPathMap.isEmpty()) {
				JSONObject requirejsConfig = new JSONObject();

				for (Entry<String, String> e : aliasToPathMap.entrySet()) {
					requirejsConfig.put(e.getKey(), e.getValue());
				}

				String script = "requirejs.config({\"paths\":" + requirejsConfig.toString() + "});";
				createScriptContent(body, script);
			}
		}

		JSONObject appModelConfig = new JSONObject();
		JSONObject settings = new JSONObject();
		settings.put("showPopoutIcon", "false");
		// settings.put("hasHeaders", "false");
		settings.put("showMaximiseIcon", "false");
		settings.put("showCloseIcon", "false");
		appModelConfig.put("settings", settings);
		appModelConfig.put("content", new ArrayList<>());
		modelTransformer.transform(body, appModel, appModelConfig);

		StringBuilder config = new StringBuilder();
		config.append("require(['goldenlayout'], function(GoldenLayout){\n");
		config.append("var config=").append(appModelConfig.toString()).append(";\n");
		config.append("var layout = new GoldenLayout(config);\n");

		stream(MPart.class, ((EObject) appModel).eAllContents()).forEach(part -> {
			String index = part.getPersistedState().get("index");

			if (index == null) {
				index = "index.html";
			}

			String path = withBundleAlias(part, index);
			config.append(String.format("layout.registerComponent('%s',"
					+ "function(container, componentState){var el=container.getElement();"//
					+ "el.load('%s')});\n", part.getElementId(), path));
		});

		stream(MToolBar.class, ((EObject) appModel).eAllContents()).forEach(toolBar -> {
			StringBuilder renderTBScript = new StringBuilder("var el=container.getElement();\n");
			renderTBScript.append("$('.lm_header').each(function(){$(this).css('display','none');});\n");
			for (MToolBarElement tbElement : toolBar.getChildren()) {
				if (tbElement instanceof MHandledToolItem) {
					MHandledToolItem mHandledToolItem = (MHandledToolItem) tbElement;
					String iconURI = withBundleAlias(mHandledToolItem, mHandledToolItem.getIconURI());
					String button = "<button class='ui-button ui-widget ui-corner-all'>" + "<img src='" + iconURI
							+ "'></img><br>" + mHandledToolItem.getLabel() + "</button>";
					renderTBScript.append(String.format("el.append(\"%s\");\n", button));
				}
			}
			String regComponentScript = String.format(
					"layout.registerComponent('%s',function(container, componentState){%s});\n", toolBar.getElementId(),
					renderTBScript);
			config.append(regComponentScript);
		});

		// config.append("$(document).ready(function(){$('.lm_header').each(function(){$(this).css('display','none')})});");
		config.append("layout.init();\n");
		config.append("});\n");
		createScriptContent(body, config.toString());
	}

	private String withBundleAlias(MApplicationElement appElement, String relativePath) {
		String bundleAliasMapping = getBundleAlias(appElement);
		// this assumes the part content is accessible via the alias
		// being the part's contributor ID or an explicit alias set via
		// the web resource manifest header
		String path = String.format("%s/%s", bundleAliasMapping, relativePath);
		return path;
	}

	private String getBundleAlias(MApplicationElement appElement) {
		String contributorURI = appElement.getContributorURI();
		String contributorId = new File(contributorURI).getName();
		String bundleAliasMapping = resReg.getBundleAliasMapping(contributorId);

		if (bundleAliasMapping == null) {
			bundleAliasMapping = contributorId;
		}

		return bundleAliasMapping;
	}

	private <T> Stream<T> stream(Class<T> type, TreeIterator<EObject> iter) {
		return (Stream<T>) StreamSupport.stream(Spliterators.spliteratorUnknownSize(iter, Spliterator.ORDERED), false)
				.filter(eo -> type.isAssignableFrom(eo.eClass().getInstanceClass()));
	}

	private void createMeta(Element head, String name, String content) {
		Element meta = head.getOwnerDocument().createElement("meta");
		meta.setAttribute("name", name);
		meta.setAttribute("content", content);
		head.appendChild(meta);
	}

	private void createLink(Element parent, String href) {
		Document doc = parent.getOwnerDocument();
		Element link = doc.createElement("link");
		link.setAttribute("rel", "stylesheet");
		link.setAttribute("type", "text/css");
		link.setAttribute("href", href);
		parent.appendChild(link);
	}

	private Element createScriptContent(Element parent, String content) {
		Document doc = parent.getOwnerDocument();
		Element script = doc.createElement("script");
		script.setAttribute("type", "text/javascript");
		script.setTextContent(content);
		parent.appendChild(script);
		return script;
	}

	private Element createScript(Element parent, String src) {
		Document doc = parent.getOwnerDocument();
		Element script = doc.createElement("script");
		script.setAttribute("type", "text/javascript");
		script.setAttribute("src", src);
		parent.appendChild(script);
		return script;
	}
}
