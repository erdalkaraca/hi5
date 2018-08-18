package de.kcct.hi5.e4;

public interface EntryPointHandlerFactory {

	String KEY = "hi5.entrypoint.handler";

	EntryPointHandler create();
}
