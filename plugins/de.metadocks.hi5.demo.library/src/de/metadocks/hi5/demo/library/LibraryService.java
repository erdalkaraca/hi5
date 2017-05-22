package de.metadocks.hi5.demo.library;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.examples.extlibrary.Book;
import org.eclipse.emf.examples.extlibrary.EXTLibraryFactory;
import org.eclipse.emf.examples.extlibrary.Library;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import de.metadocks.hi5.jaxrs.JSApiProvider;

@Component(service = LibraryService.class)
@Path("/library-service")
public class LibraryService implements JSApiProvider {

	private Library lib;

	@Activate
	protected void activate() {
		lib = EXTLibraryFactory.eINSTANCE.createLibrary();
		lib.setName("My media library");
		lib.setId(EcoreUtil.generateUUID());
		for (int i = 0; i < 3; i++) {
			Book book = EXTLibraryFactory.eINSTANCE.createBook();
			book.setId(EcoreUtil.generateUUID());
			book.setTitle("Book " + i);
			lib.getStock().add(book);
		}
	}

	@GET
	@Path("/get-books")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Book> getBooks() {
		return new ArrayList<Book>(lib.getBooks());
	}
}
