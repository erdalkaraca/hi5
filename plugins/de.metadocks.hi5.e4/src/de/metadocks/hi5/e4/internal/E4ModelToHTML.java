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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.SideValue;
import org.eclipse.e4.ui.model.application.ui.advanced.MArea;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;

public class E4ModelToHTML {
	private static final List<EAttribute> PUBLIC_ATTRS = Arrays.asList();
	
	private Map<Class<? extends MUIElement>, Rule<? extends MUIElement>> grammar = new HashMap<>();
	{
		registerRule(MElementContainer.class, ctx -> {
			handleElementContainer(ctx, childContentConfig -> {
				childContentConfig.put("type", "column");
			});
		});

		registerRule(MPartStack.class, ctx -> {
			handleElementContainer(ctx, childContentConfig -> {
				childContentConfig.put("type", "stack");
			});
		});

		Rule<MPartSashContainer> partSashContainerRule = ctx -> {
			handleElementContainer(ctx, childContentConfig -> {
				childContentConfig.put("type", ctx.modelElement.isHorizontal() ? "row" : "column");
			});
		};
		registerRule(MPartSashContainer.class, partSashContainerRule);
		registerRule(MArea.class, partSashContainerRule);

		registerRule(MPart.class, ctx -> {
			handleContentItem(ctx, childContentConfig -> {
				childContentConfig.put("componentName", ctx.modelElement.getElementId());
				childContentConfig.put("title", ctx.modelElement.getLabel());
			});
		});

		registerRule(MTrimmedWindow.class, ctx -> {
			List<MTrimBar> trimBars = ctx.modelElement.getTrimBars();

			Consumer<? super MTrimBar> horizontalTrimBarConsumer = tb -> {
				RuleContext<MUIElement> childCtx = new RuleContext<>();
				childCtx.appModelConfig = ctx.appModelConfig;
				childCtx.parent = ctx.parent;
				childCtx.modelElement = tb;

				try {
					callRule(MTrimBar.class, childCtx);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			};

			// TODO left and right trimbars

			trimBars.stream().filter(tb -> tb.getSide() == SideValue.TOP).forEach(horizontalTrimBarConsumer);
			callRule(MElementContainer.class, ctx);
			trimBars.stream().filter(tb -> tb.getSide() == SideValue.BOTTOM).forEach(horizontalTrimBarConsumer);
		});

		registerRule(MTrimBar.class, ctx -> {
			handleElementContainer(ctx, childContentConfig -> {
				childContentConfig.put("type", "row");
			});
		});

		registerRule(MToolBar.class, ctx -> {
			handleContentItem(ctx, childContentConfig -> {
				childContentConfig.put("componentName", ctx.modelElement.getElementId());
				childContentConfig.put("title", ctx.modelElement.getElementId());
			});
		});
	}

	private void handleContentItem(RuleContext<? extends MUIElement> ctx, CheckedConumser<JSONObject> consumer)
			throws JSONException {
		JSONArray contentCols = ctx.appModelConfig.getJSONArray("content");
		JSONObject childContentConfig = new JSONObject();
		contentCols.put(childContentConfig);
		childContentConfig.put("type", "component");
		childContentConfig.put("id", ctx.modelElement.getElementId());
		childContentConfig.put("cssClass", ((EObject) ctx.modelElement).eClass().getName());

		try {
			consumer.accept(childContentConfig);
		} catch (Exception e) {
			e.printStackTrace();
		}
		JSONObject componentState = new JSONObject();
		childContentConfig.put("componentState", componentState);
	}

	@SuppressWarnings("rawtypes")
	private void handleElementContainer(RuleContext<? extends MElementContainer> ctx,
			CheckedConumser<JSONObject> consumer) throws JSONException {
		JSONArray contentCols = ctx.appModelConfig.getJSONArray("content");
		JSONObject childContentConfig = new JSONObject();
		childContentConfig.put("id", ctx.modelElement.getElementId());
		childContentConfig.put("content", new ArrayList<Object>());
		contentCols.put(childContentConfig);

		try {
			consumer.accept(childContentConfig);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		List<MUIElement> children = ctx.modelElement.getChildren();

		for (MUIElement child : children) {
			transform(ctx.parent, child, childContentConfig);
		}
	}

	private Element createElement(Element xParent, String elementName, String elementClass, Map<String, String> atts) {
		Element element = xParent.getOwnerDocument().createElement(elementName);
		element.setAttribute("class", elementClass);

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

	private Element createDiv(Element xParent, MUIElement modelElement) {
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
		atts.put("id", modelElement.getElementId());
		Element element = createElement(xParent, "div", ((EObject) modelElement).eClass().getName(), atts);
		return element;
	}

	private <T extends MUIElement> void callRule(Class<?> type, RuleContext<T> ctx) throws JSONException {
		Rule<T> rule = (Rule<T>) grammar.get(type);
		rule.apply(ctx);
	}

	public <T extends MUIElement> void transform(Element xParent, T modelElement, JSONObject appModelConfig)
			throws JSONException {
		Rule<T> consumer = (Rule<T>) grammar.get(((EObject) modelElement).eClass().getInstanceClass());

		if (consumer == null && modelElement instanceof MElementContainer<?>) {
			consumer = (Rule<T>) grammar.get(MElementContainer.class);
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

	private <T extends MUIElement, S extends T> void registerRule(Class<S> type, Rule<T> rule) {
		grammar.put(type, rule);
	}

	private static class RuleContext<T extends MUIElement> {
		T modelElement;
		Element parent;
		JSONObject appModelConfig;
	}

	private static interface Rule<T extends MUIElement> {
		void apply(RuleContext<T> ctx) throws JSONException;
	}

	private static interface CheckedConumser<T> {
		void accept(T param) throws Exception;
	}
}
