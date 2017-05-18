package de.metadocks.hi5.demo.library;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.emf.examples.extlibrary.Book;
import org.eclipse.emf.examples.extlibrary.EXTLibraryFactory;
import org.eclipse.emf.examples.extlibrary.Library;
import org.osgi.service.component.annotations.Component;

import de.metadocks.hi5.jaxrs.JSApiProvider;

@Component(service = LibraryService.class)
@Path("/library-service")
public class LibraryService implements JSApiProvider {

	@GET
	@Path("/get-library")
	public Library getLibrary() {
		Library lib = EXTLibraryFactory.eINSTANCE.createLibrary();
		lib.setName("My virtual library");
		
		Book book = EXTLibraryFactory.eINSTANCE.createBook();
		book.setTitle("Book 1");
		lib.getBooks().add(book);
		
		return lib;
	}
}
