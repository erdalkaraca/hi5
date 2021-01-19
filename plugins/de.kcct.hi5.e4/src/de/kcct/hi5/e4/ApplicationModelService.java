package de.kcct.hi5.e4;

import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.kcct.hi5.e4.internal.E4Runtime;

@Component(immediate = true, service = ApplicationModelService.class)
public class ApplicationModelService {
	@Reference
	private E4Runtime e4;
	
	public MApplicationElement getModelElement(String id) {
		return e4.getModelElement(id);
	}
}
