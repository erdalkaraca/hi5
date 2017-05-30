package de.metadocks.hi5.demo.library;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.examples.extlibrary.Book;
import org.eclipse.emf.examples.extlibrary.EXTLibraryFactory;
import org.eclipse.emf.examples.extlibrary.Library;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component(service = LibraryService.class)
@Path("/library-service")
public class LibraryService {

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
	public List<Book> getBooks() {
		return lib.getBooks();
	}
}
