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
package de.kcct.hi5.jaxrs.emf;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.eclipse.emf.ecore.EObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true)
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class EMFMessageBodyReader implements MessageBodyReader<EObject> {
	@Reference
	private EMFConverterService converterService;

	@Override
	public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
		return EObject.class.isAssignableFrom(arg0);
	}

	@Override
	public EObject readFrom(Class<EObject> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {
		return converterService.toEObject(type, entityStream);
	}
}
