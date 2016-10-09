package de.metadocks.hi5.jaxrs.jersey;

public interface FactoryExt<T> {
	Class<T> getType();

	T provide();

	void dispose(T obj);

}
