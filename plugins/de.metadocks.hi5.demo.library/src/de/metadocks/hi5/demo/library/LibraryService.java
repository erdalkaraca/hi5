package de.metadocks.hi5.demo.library;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.metadocks.demo.model.lib.Book;
import de.metadocks.demo.model.lib.LibFactory;
import de.metadocks.demo.model.lib.Library;
import de.metadocks.demo.model.lib.Person;

@Component(service = LibraryService.class)
@Path("/library-service")
public class LibraryService {

	private Library lib;

	@Activate
	protected void activate(BundleContext context) throws Exception {
		// source:
		// https://github.com/lsharalieva/basex-examples/blob/master/etc/xml/books.xml
		URL booksXml = context.getBundle().getEntry("data/books.xml");
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(booksXml.openStream());
		XPathFactory xPathfactory = XPathFactory.newInstance();
		XPath xpath = xPathfactory.newXPath();
		XPathExpression expr = xpath.compile("//book");
		NodeList books = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

		lib = LibFactory.eINSTANCE.createLibrary();
		lib.setName("My media library");
		lib.setId(EcoreUtil.generateUUID());
		SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd");

		for (int i = 0; i < books.getLength(); i++) {
			Node bookNode = books.item(i);
			Book book = LibFactory.eINSTANCE.createBook();
			book.setId(EcoreUtil.generateUUID());
			book.setTitle(xpath.compile("title/text()").evaluate(bookNode));
			book.setPublicationDate(sdf.parse(xpath.compile("publish_date/text()").evaluate(bookNode)));
			book.setPages((int) (Math.random() * 1000));
			book.setPrice(Float.parseFloat(xpath.compile("price/text()").evaluate(bookNode)));
			lib.getBooks().add(book);

			Person writer = LibFactory.eINSTANCE.createPerson();
			writer.setName(xpath.compile("author/text()").evaluate(bookNode));
			lib.getPersons().add(writer);
			book.setAuthor(writer);
		}
	}

	@GET
	@Path("/get-books")
	public List<Book> getBooks() {
		return lib.getBooks();
	}
}
