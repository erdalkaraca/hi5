package de.kcct.hi5.jaxrs.emf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.emf.ecore.EObject;
import org.emfjson.jackson.module.EMFModule;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component(immediate = true, service = EMFConverterService.class)
public class EMFConverterService {
	private ObjectMapper mapper;

	@Activate
	protected void activate() {
		mapper = EMFModule.setupDefaultMapper();
	}

	public <T extends EObject> T toEObject(Class<T> type, InputStream entityStream) {
		try {
			return mapper.readValue(entityStream, type);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void toJSONString() {

	}

	public void writeJSON(EObject entity, OutputStream entityStream) {
		try {
			mapper.writeValue(entityStream, entity);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String toJSON(EObject entity) {
		try {
			return mapper.writeValueAsString(entity);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
