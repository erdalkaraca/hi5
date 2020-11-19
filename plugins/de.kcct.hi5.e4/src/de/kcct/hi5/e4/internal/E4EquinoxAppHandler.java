package de.kcct.hi5.e4.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.kcct.hi5.internal.WebResourcesRegistry;

@Component(service = E4EquinoxAppHandler.class)
public class E4EquinoxAppHandler {
	@Reference
	private WebResourcesRegistry resReg;

	@Reference
	private E4Runtime e4runtime;
}
