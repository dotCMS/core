
package com.dotmarketing.business.portal;

import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.Portlet;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Provides low-level access to information related to {@link Portlet} objects in dotCMS.
 *
 * @author Erick Gonzalez
 * @since Apr 24th, 2019
 */
public interface PortletFactory {

  /**
   * Returns a collection with all the available portlets in the current dotCMS instance. They can
   * be defined via the {@code /WEB-INF/portlet.xml} file, or retrieved directly from the
   * database.
   *
   * @return A collection with {@link Portlet} objects.
   *
   * @throws SystemException An error occurred while retrieving the portlets.
   */
  Collection<Portlet> getPortlets() throws SystemException;

  /**
   * Finds a portlet by its ID.
   *
   * @param portletId The ID of the portlet to find.
   *
   * @return The {@link Portlet} with the specified ID.
   */
  Portlet findById(final String portletId) ;

  /**
   * Updates the specified portlet in the database.
   *
   * @param portlet The {@link Portlet} to update.
   *
   * @return The updated {@link Portlet} object.
   *
   * @throws DotDataException An error occurred while updating the portlet.
   */
  Portlet updatePortlet(final Portlet portlet) throws DotDataException;

  /**
   * Deletes the specified portlet from the database.
   *
   * @param portletId The ID of the portlet to delete.
   *
   * @throws DotDataException An error occurred while deleting the portlet.
   */
  void deletePortlet(final String portletId) throws DotDataException;

  /**
   * Saves the specified custom portlet in the database.
   *
   * @param portlet The {@link Portlet} object to insert.
   *
   * @return The inserted {@link Portlet} object.
   *
   * @throws DotDataException An error occurred while persisting the portlet.
   */
  Portlet insertPortlet(final Portlet portlet) throws DotDataException;

  /**
   * Transforms the specified Portlet object into an XML representation. This way, its definition
   * can be persisted to the database and can be loaded any time as required.
   *
   * @param portlet The {@link Portlet} object to transform.
   *
   * @return The XML representation of the Portlet object.
   *
   * @throws DotDataException An error occurred while transforming the Portlet object.
   */
  String portletToXml(final Portlet portlet) throws DotDataException;

  /**
   * Reads the specified array of XML files and extracts the Portlet definitions form each of
   * them. Finally, transforms their attributes into a data map that is used by dotCMS to load
   * them into the system.
   *
   * @param xmlFiles The array of XML files to read.
   *
   * @return A map with the Portlet definitions extracted from the XML files.
   *
   * @throws SystemException An error occurred while mapping the XML data.
   */
  Map<String, Portlet> xmlToPortlets(String[] xmlFiles) throws SystemException;

  Map<String, Portlet> xmlToPortlets(InputStream[] xmlFiles) throws SystemException;

  /**
   * Transforms the specified XML string into a Portlet object. This allows the API to read
   * portlets that are defined in configuration files or the database.
   *
   * @param xml The XML string to transform.
   *
   * @return An optional {@link DotPortlet} object.
   *
   * @throws IOException   An error occurred when transforming the XML into an Input Stream.
   * @throws JAXBException An error occurred when mapping the XML String to a {@link DotPortlet}
   *                       object.
   */
  Optional<Portlet> xmlToPortlet(final String xml) throws IOException, JAXBException;

}
