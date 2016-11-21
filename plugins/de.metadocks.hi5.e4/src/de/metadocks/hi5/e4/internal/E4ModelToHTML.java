/*******************************************************************************
 * Copyright (c) 2016 Erdal Karaca.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Erdal Karaca <erdal.karaca.de@gmail.com> - initial API and implementation
 *******************************************************************************/
package de.metadocks.hi5.e4.internal;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.application.impl.ApplicationPackageImpl;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.SideValue;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.model.application.ui.impl.UiPackageImpl;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@Component(immediate = true, service = E4ModelToHTML.class)
public class E4ModelToHTML {
	@SuppressWarnings("restriction")
	private static final List<EAttribute> PUBLIC_ATTRS = Arrays.asList(
			ApplicationPackageImpl.Literals.CONTRIBUTION__CONTRIBUTION_URI,
			ApplicationPackageImpl.Literals.APPLICATION_ELEMENT__ELEMENT_ID,
			UiPackageImpl.Literals.UI_ELEMENT__CONTAINER_DATA, UiPackageImpl.Literals.UI_LABEL__LABEL,
			UiPackageImpl.Literals.UI_LABEL__ICON_URI);

	private WebResourcesRegistry resReg;
	private Map<Class<? extends MApplicationElement>, Rule<? extends MApplicationElement>> grammar = new HashMap<>();
	private DocumentBuilder builder;
	private Transformer transformer;

	@Activate
	public void activate() throws ParserConfigurationException, TransformerConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(false);
		dbf.setNamespaceAware(true);
		dbf.setIgnoringComments(false);
		dbf.setIgnoringElementContentWhitespace(false);
		dbf.setExpandEntityReferences(false);

		builder = dbf.newDocumentBuilder();
		TransformerFactory tf = TransformerFactory.newInstance();
		transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

		registerRule(MElementContainer.class, ctx -> {
			handleElementContainer(ctx);
		});

		registerRule(MTrimmedWindow.class, ctx -> {
			Element newParent = createDiv(ctx.parent, ctx.modelElement);
			// process main menu
			transform(newParent, ctx.modelElement.getMainMenu(), ctx.appModelConfig);

			Consumer<? super MTrimBar> action = tb -> {
				try {
					transform(newParent, tb, ctx.appModelConfig);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			};
			ctx.modelElement.getTrimBars().stream().filter(tb -> tb.getSide() == SideValue.TOP).forEach(action);
			ctx.modelElement.getTrimBars().stream().filter(tb -> tb.getSide() == SideValue.LEFT).forEach(action);

			for (MUIElement child : ctx.modelElement.getChildren()) {
				transform(newParent, child, ctx.appModelConfig);
			}

			ctx.modelElement.getTrimBars().stream().filter(tb -> tb.getSide() == SideValue.RIGHT).forEach(action);
			ctx.modelElement.getTrimBars().stream().filter(tb -> tb.getSide() == SideValue.BOTTOM).forEach(action);
		});

		registerRule(MTrimBar.class, ctx -> {
			Element trimBarElement = handleElementContainer(ctx);
			String direction = ctx.modelElement.getSide().name();
			trimBarElement.setAttribute("direction", direction);
		});

		registerRule(MApplicationElement.class, ctx -> {
			handleLeaf(ctx);
		});

		registerRule(MPart.class, ctx -> {
			Element newParent = handleLeaf(ctx);

			for (MMenu menu : ctx.modelElement.getMenus()) {
				transform(newParent, menu, ctx.appModelConfig);
			}

			MToolBar toolbar = ctx.modelElement.getToolbar();
			if (toolbar != null) {
				transform(newParent, toolbar, ctx.appModelConfig);
			}
		});
	}

	@Reference(unbind = "-")
	public void setResReg(WebResourcesRegistry resReg) {
		this.resReg = resReg;
	}

	public String toHTML(MUIElement element) throws JSONException {
		JSONObject appModelConfig = new JSONObject();
		Document doc = builder.newDocument();
		Element container = doc.createElement("div");
		doc.appendChild(container);
		transform(container, element, appModelConfig);
		Node firstChild = container.getFirstChild();
		StringWriter writer = new StringWriter();

		try {
			transformer.transform(new DOMSource(firstChild), new StreamResult(writer));
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return writer.toString();
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

	private Element handleLeaf(RuleContext<? extends MApplicationElement> ctx) throws JSONException {
		Element newParent = createDiv(ctx.parent, ctx.modelElement);
		return newParent;
	}

	@SuppressWarnings("rawtypes")
	private Element handleElementContainer(RuleContext<? extends MElementContainer> ctx) throws JSONException {
		Element newParent = createDiv(ctx.parent, ctx.modelElement);
		List<MUIElement> children = ctx.modelElement.getChildren();

		for (MUIElement child : children) {
			transform(newParent, child, ctx.appModelConfig);
		}

		return newParent;
	}

	private Element createElement(Element xParent, String elementName, EClass eClass, Map<String, String> atts) {
		Element element = xParent.getOwnerDocument().createElement(elementName);
		element.setAttribute("etype", eClass.getName());

		Set<String> classes = new HashSet<>();
		classes.add(eClass.getName());

		for (EClass ecls : eClass.getEAllSuperTypes()) {
			classes.add(ecls.getName());
		}

		String htmlClass = classes.stream().collect(Collectors.joining(" "));
		element.setAttribute("class", htmlClass);

		for (Entry<String, String> e : atts.entrySet()) {
			element.setAttribute(e.getKey(), e.getValue());
		}

		xParent.appendChild(element);
		return element;
	}

	private void addClass(Element div, String... htmlClasses) {
		if (htmlClasses == null || htmlClasses.length == 0) {
			return;
		}

		String existingClass = div.getAttribute("class");
		ArrayList<String> listOfClasses = new ArrayList<String>();

		if (existingClass != null) {
			listOfClasses.add(existingClass);
		}

		listOfClasses.addAll(Arrays.asList(htmlClasses));
		String newClass = listOfClasses.stream().collect(Collectors.joining(" "));
		div.setAttribute("class", newClass);
	}

	private Element createDiv(Element xParent, MApplicationElement modelElement) {
		Map<String, String> atts = new HashMap<>();
		EObject eo = (EObject) modelElement;

		if (!PUBLIC_ATTRS.isEmpty()) {
			List<EAttribute> eAllAttributes = eo.eClass().getEAllAttributes();

			for (EAttribute attr : eAllAttributes) {
				if (!PUBLIC_ATTRS.contains(attr)) {
					continue;
				}

				if (!eo.eIsSet(attr)) {
					continue;
				}

				Object val = eo.eGet(attr);

				if (val == null) {
					val = "";
				}

				atts.put(attr.getName(), val.toString());
			}
		}

		// bind MUIElement elementId to DOM id
		String elementId = modelElement.getElementId();

		if (elementId != null && !elementId.isEmpty()) {
			elementId = elementId.replace('.', '_');
			atts.put("id", elementId);
		}

		String contributorURI = modelElement.getContributorURI();

		if (contributorURI != null && !contributorURI.isEmpty()) {
			contributorURI = getBundleAlias(modelElement);
			atts.put(ApplicationPackageImpl.Literals.APPLICATION_ELEMENT__CONTRIBUTOR_URI.getName(), contributorURI);
		}

		Element element = createElement(xParent, "div", ((EObject) modelElement).eClass(), atts);

		for (Entry<String, String> e : modelElement.getPersistedState().entrySet()) {
			element.setAttribute("state_" + e.getKey(), e.getValue());
		}

		String tags = modelElement.getTags().stream().collect(Collectors.joining(" "));

		if (!tags.isEmpty()) {
			element.setAttribute("tags", tags);
		}

		return element;
	}

	public <T extends MUIElement> void transform(Element xParent, T modelElement, JSONObject appModelConfig)
			throws JSONException {
		Rule<T> consumer = (Rule<T>) grammar.get(((EObject) modelElement).eClass().getInstanceClass());

		if (consumer == null && modelElement instanceof MElementContainer<?>) {
			consumer = (Rule<T>) grammar.get(MElementContainer.class);
		}

		if (consumer == null && modelElement instanceof MApplicationElement) {
			consumer = (Rule<T>) grammar.get(MApplicationElement.class);
		}

		if (consumer == null) {
			System.err.println("No rule found for: " + ((EObject) modelElement).eClass().getName());
			return;
		}

		RuleContext<T> ctx = new RuleContext<>();
		ctx.modelElement = modelElement;
		ctx.parent = xParent;
		ctx.appModelConfig = appModelConfig;
		consumer.apply(ctx);
	}

	private <T extends MApplicationElement, S extends T> void registerRule(Class<S> type, Rule<T> rule) {
		grammar.put(type, rule);
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

	public void setResourcesRegistry(WebResourcesRegistry resReg) {
		this.resReg = resReg;
	}

	private static class RuleContext<T extends MApplicationElement> {
		T modelElement;
		Element parent;
		JSONObject appModelConfig;
	}

	private static interface Rule<T extends MApplicationElement> {
		void apply(RuleContext<T> ctx) throws JSONException;
	}

	private static interface CheckedConumser<T> {
		void accept(T param) throws Exception;
	}
}
