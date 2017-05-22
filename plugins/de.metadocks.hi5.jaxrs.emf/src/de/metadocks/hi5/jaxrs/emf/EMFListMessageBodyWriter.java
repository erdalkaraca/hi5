/*******************************************************************************
 * Copyright (c) 2017 Erdal Karaca.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Erdal Karaca <erdal.karaca.de@gmail.com> - initial API and implementation
 *******************************************************************************/
package de.metadocks.hi5.jaxrs.emf;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.texo.json.EMFJSONConverter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;

@Component(service = MessageBodyWriter.class, immediate = true)
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class EMFListMessageBodyWriter implements MessageBodyWriter<List<EObject>> {

	@Override
	public long getSize(List<EObject> arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
		return arg0.size();
	}

	@Override
	public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
		return MediaType.APPLICATION_JSON_TYPE.equals(arg3);
	}

	@Override
	public void writeTo(List<EObject> arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4,
			MultivaluedMap<String, Object> arg5, OutputStream arg6) throws IOException, WebApplicationException {
		EMFJSONConverter converter = new EMFJSONConverter();
		converter.setConvertNonContainedReferencedObjects(true);
		converter.setSerializeTitleProperty(false);
		converter.setMaxChildLevelsToConvert(1);
		JSONArray array = new JSONArray();
		for (EObject eObject : arg0) {
			JSONObject jObj = (JSONObject) converter.convert((EObject) eObject);
			array.put(jObj);
		}

		try {
			OutputStreamWriter writer = new OutputStreamWriter(arg6);
			array.write(writer);
			writer.flush();
		} catch (JSONException e) {
			throw new IOException(e);
		}
	}
}
