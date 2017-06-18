/**
 */
package de.metadocks.demo.model.lib;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Library</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link de.metadocks.demo.model.lib.Library#getName <em>Name</em>}</li>
 *   <li>{@link de.metadocks.demo.model.lib.Library#getBooks <em>Books</em>}</li>
 *   <li>{@link de.metadocks.demo.model.lib.Library#getPersons <em>Persons</em>}</li>
 * </ul>
 *
 * @see de.metadocks.demo.model.lib.LibPackage#getLibrary()
 * @model
 * @generated
 */
public interface Library extends Identifiable {
	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Name</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see de.metadocks.demo.model.lib.LibPackage#getLibrary_Name()
	 * @model
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link de.metadocks.demo.model.lib.Library#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Books</b></em>' reference list.
	 * The list contents are of type {@link de.metadocks.demo.model.lib.Book}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Books</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Books</em>' reference list.
	 * @see de.metadocks.demo.model.lib.LibPackage#getLibrary_Books()
	 * @model transient="true" derived="true" ordered="false"
	 * @generated
	 */
	EList<Book> getBooks();

	/**
	 * Returns the value of the '<em><b>Persons</b></em>' containment reference list.
	 * The list contents are of type {@link de.metadocks.demo.model.lib.Person}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Persons</em>' reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Persons</em>' containment reference list.
	 * @see de.metadocks.demo.model.lib.LibPackage#getLibrary_Persons()
	 * @model containment="true"
	 * @generated
	 */
	EList<Person> getPersons();

} // Library
