package com.dotmarketing.business.portal;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.PrincipalBean;
import com.liferay.portal.model.Portlet;
import com.liferay.util.FileUtil;
import io.vavr.control.Try;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Implementation class for the {@link PortletFactory} interface. This class uses JAXB to map XML
 * files into Java objects.
 * <p>By default, portlet definitions come from three sources:
 * <ul>
 *     <li>The {@code "/WEB-INF/portlet.xml"} file.</li>
 *     <li>The {@code "/WEB-INF/portlet-ext.xml"} file.</li>
 *     <li>The database, where custom Portlets are stored.</li>
 * </ul>
 * </p>
 *
 * @author Erick Gonzalez
 * @since Apr 24th, 2019
 */
public class PortletFactoryImpl extends PrincipalBean implements PortletFactory {

  private final String[] systemXmlFiles;

  private static final Map<Class<?>, JaxbContext> jaxbContexts;

  // Creates the JAXB contexts for the {@link DotPortlet} and {@link PortletList} classes. They're
  // used by JAXB to map the contents of the configuration XML files into such classes.
  static {
    jaxbContexts = Try.of(() -> Map.of(
                    DotPortlet.class, new JaxbContext(DotPortlet.class),
                    PortletList.class, new JaxbContext(PortletList.class)))
            .getOrElse(Map.of());
  }

  /**
   * Creates an instance of this Factory using the specified XML files as sources of Portlet
   * definitions.
   *
   * @param systemXmlFiles The paths to the XML files to read.
   */
  public PortletFactoryImpl(final String[] systemXmlFiles) {
    this.systemXmlFiles = systemXmlFiles;
  }

  /**
   * Default class constructor. It sets the default paths to the system XML files.
   */
  public PortletFactoryImpl() {
    this(new String[] {FileUtil.getRealPath("/WEB-INF/portlet.xml"), FileUtil.getRealPath("/WEB-INF/portlet-ext.xml")});
  }

  /**
   * Reads the contents of the specified XML file, and maps the Portlet definitions from it into
   * Portlet objects.
   *
   * @param pathToXmlFile The path to the XML file to read.
   *
   * @return A map with the {@link Portlet} definitions.
   *
   * @throws IOException   An error occurred while reading the XML file.
   * @throws JAXBException An error occurred while mapping the XML file into Java objects.
   */
  private Map<String, Portlet> xmlToPortlets(final String pathToXmlFile) throws IOException, JAXBException {
    if (UtilMethods.isNotSet(pathToXmlFile)) {
      return new HashMap<>();
    }
    Logger.debug(this, "Loading Portlets from file: " + pathToXmlFile);
    final InputStream stream = new FileInputStream(pathToXmlFile);
    return xmlToPortlets(stream);
  }

  /**
   * Takes the Input Stream of an XML file and extracts the Portlet definitions from it into
   * Portlet objects.
   *
   * @param fileStream The {@link InputStream} of the XML file to read.
   *
   * @return A map with the {@link Portlet} definitions.
   *
   * @throws JAXBException An error occurred while mapping the XML file into Java objects.
   */
  private Map<String, Portlet> xmlToPortlets(final InputStream fileStream) throws JAXBException {
    if (null == fileStream) {
      return new HashMap<>();
    }
    final Map<String, Portlet> portlets = new HashMap<>();
    final PortletList portletList = (PortletList) jaxbContexts.get(PortletList.class).unmarshall(fileStream);
    int counter = 1;
    if (UtilMethods.isSet(portletList) && UtilMethods.isSet(portletList.getPortlets())) {
      for (final DotPortlet portlet : portletList.getPortlets()) {
        portlets.put(portlet.getPortletId(), portlet);
        Logger.debug(this, String.format("%d. Loading portlet ID '%s'", counter, portlet.getPortletId()));
        counter++;
    }
      }
    return portlets;
  }

  @Override
  @VisibleForTesting
  public Optional<DotPortlet> xmlToPortlet(final String xml) throws JAXBException {
    final InputStream stream = new ByteArrayInputStream(xml.getBytes(UTF_8));
    final DotPortlet portlet = (DotPortlet) jaxbContexts.get(DotPortlet.class).unmarshall(stream);
    if (null == portlet.getPortletId() || null == portlet.getPortletClass()) {
      return Optional.empty();
    }
    return Optional.of(portlet);
  }

  @Override
  public Map<String, Portlet> xmlToPortlets(final String[] xmlFiles) throws SystemException {
    final Map<String, Portlet> portlets = new HashMap<>();
    for (final String xmlFile : xmlFiles) {
      try {
        portlets.putAll(xmlToPortlets(xmlFile));
      } catch (final Exception e) {
        throw new SystemException(String.format("An error occurred when loading portlets from XML file " +
                "'%s': %s", xmlFile, ExceptionUtil.getErrorMessage(e)), e);
      }
    }
    return portlets;
  }

  @Override
  public Map<String, Portlet> xmlToPortlets(final InputStream[] xmlFiles) throws SystemException {
    final Map<String, Portlet> portlets = new HashMap<>();
    for (final InputStream xmlFile : xmlFiles) {
      try {
        portlets.putAll(xmlToPortlets(xmlFile));
      } catch (final Exception e) {
        throw new SystemException(String.format("An error occurred when loading portlets from XML stream: " +
                "%s", ExceptionUtil.getErrorMessage(e)), e);
      }
    }
    return portlets;
  }

  @WrapInTransaction
  @Override
  public void deletePortlet(final String portletId) throws DotDataException {
    final DotConnect db = new DotConnect();
    db.setSQL("delete from portletpreferences where portletid=?").addParam(portletId).loadResult();
    db.setSQL("delete from portlet where portletid=?").addParam(portletId).loadResult();
    db.setSQL("delete from cms_layouts_portlets where portlet_id=?" ).addParam(portletId).loadResult();
    CacheLocator.getPortletCache().clearCache();
    CacheLocator.getLayoutCache().clearCache();
    APILocator.getSystemEventsAPI().pushAsync(SystemEventType.UPDATE_PORTLET_LAYOUTS, new Payload());
  }

  @Override
  public Portlet findById(final String portletId) {
    return getPortletMap().get(portletId);
  }

  /**
   * Loads the dotCMS portlets from both the system XML files and the database.
   *
   * @return A map with the portlets loaded from the system XML files and the database.
   */
  private Map<String, Portlet> loadSystemPortlets() {
    final Map<String, Portlet> portlets = new HashMap<>();

    try {
      portlets.putAll(xmlToPortlets(this.systemXmlFiles));
    } catch (final Exception e) {
      Logger.error(this, e.getMessage(), e);
    }

    final List<Portlet> portletList = Try.of(this::findAllDb).getOrElse(Lists.newArrayList());
    for (final Portlet portlet : portletList) {
      portlets.put(portlet.getPortletId(), portlet);
    }
    return portlets;
  }

  /**
   * Returns the available Portlets in the current dotCMS instance. If the Portlets are not loaded
   * in cache yet, it takes care of doing so.
   *
   * @return A map with the available Portlets.
   */
  private Map<String, Portlet> getPortletMap() {
    final PortletCache cache = new PortletCache();
    Map<String, Portlet> portletsMap = cache.getAllPortlets();
    if (portletsMap.isEmpty()) {
      synchronized (PortletCache.class) {
        portletsMap = cache.getAllPortlets();
        if (portletsMap.isEmpty()) {
          final Map<String, Portlet> portletMap = this.loadSystemPortlets();
          portletsMap.putAll(portletMap);
          cache.putAllPortlets(portletsMap);
        }
      }
    }
    return portletsMap;
  }

  @Override
  public Collection<Portlet> getPortlets() throws SystemException {
    return getPortletMap().values();
  }

  /**
   * Loads all Portlets directly from the database. Keep in mind that Users can create their own
   * portlets for displaying different contents of a given type; e.g., Videos, PDFs, etc.
   *
   * @return A list with all the Portlets stored in the database.
   *
   * @throws DotDataException An error occurred while loading the Portlets from the database.
   */
  @CloseDBIfOpened
  public List<Portlet> findAllDb() throws DotDataException {
    final DotConnect db = new DotConnect();
    db.setSQL("select * from portlet where companyid=?").addParam("dotcms.org");
    final List<Portlet> portlets = new ArrayList<>();
    final List<Map<String, Object>> portletsFromDb = db.loadObjectResults();
    for (final Map<String, Object> portletData : portletsFromDb) {
      final Portlet testPortlet = new Portlet((String) portletData.get("portletid"), (String) portletData.get("groupid"), (String) portletData.get("companyid"),
          (String) portletData.get("defaultpreferences"), false, (String) portletData.get("roles"), true);

      try {
        final Optional<DotPortlet> xmlPortlet = xmlToPortlet((String) portletData.get("defaultpreferences"));
        xmlPortlet.ifPresent(portlets::add);
      } catch (final UnmarshalException e) {
        Logger.debug(this.getClass(), String.format("XML code for Portlet ID '%s'" +
                " cannot be mapped to DotPortlet class: %s", testPortlet.getPortletId(), ExceptionUtil.getErrorMessage(e)));
      } catch (final Exception e) {
        Logger.warn(this.getClass(), String.format("Unable to parse XML code to Portlet with ID '%s': %s",
                testPortlet.getPortletId(), ExceptionUtil.getErrorMessage(e)));
        }
    }
    return portlets;
  }

  @WrapInTransaction
  @Override
  public Portlet insertPortlet(final Portlet portlet) throws DotDataException {
    if (doesPortletExistInDb(portlet)) {
      return updatePortlet(portlet);
    }
    final String portletXML = portletToXml(portlet);
    new DotConnect().setSQL(
        "insert into portlet (portletid, groupid, companyid, defaultpreferences, narrow, active_) values(?,?,?,?,?,?)")
        .addParam(portlet.getPortletId())
            .addParam(UtilMethods.isSet(portlet.getGroupId())?portlet.getGroupId():"SHARED_KEY")
            .addParam(portlet.getCompanyId())
            .addParam(portletXML)
            .addParam(portlet.getNarrow())
            .addParam(portlet.getActive())
            .loadResult();
    new PortletCache().clear();
    return portlet;
  }

  @WrapInTransaction
  @Override
  public Portlet updatePortlet(final Portlet portlet) throws DotDataException {
    if (!doesPortletExistInDb(portlet)) {
      return insertPortlet(portlet);
    }
    final String portletXML = portletToXml(portlet);
    final DotConnect db = new DotConnect();
    db.setSQL("update portlet set groupid=?, defaultpreferences=? where portletid=?")
            .addParam(portlet.getGroupId())
            .addParam(portletXML)
            .addParam(portlet.getPortletId())
            .loadResult();
    new PortletCache().clear();
    return portlet;
  }

  /**
   * Checks whether the specified Portlet already exists in the database or not.
   *
   * @param portlet The {@link Portlet} to check.
   *
   * @return If the specified Portlet already exists, returns {@code true}.
   */
  @CloseDBIfOpened
  private boolean doesPortletExistInDb(final Portlet portlet) {
        return (new DotConnect().setSQL("select count(*) as test from portlet where portletid=?")
                .addParam(portlet.getPortletId())
        .getInt("test") > 0);
  }

  @Override
  public String portletToXml(final Portlet portlet) throws DotDataException {
    try {
        final StringWriter sw = new StringWriter();
        final Marshaller jaxbMarshaller = jaxbContexts.get(DotPortlet.class).getMarshaller();
        jaxbMarshaller.marshal(new DotPortlet(portlet), sw);
        return sw.toString();
    } catch (final JAXBException e) {
        Logger.error(this, String.format("Failed to transform the Portlet with ID " +
                "'%s' into XML: %s", portlet.getPortletId(), ExceptionUtil.getErrorMessage(e)));
        throw new DotDataException(e);
    }
  }

  /**
   * A simple class to hold the JAXB context and its respective Marshaller and Unmarshaller
   * implementations for a specific class.
   */
  private static class JaxbContext {

    final JAXBContext jaxbContextObj;
    final Unmarshaller unmarshaller;
    final Marshaller marshaller;

    /**
     * Default class constructor.
     *
     * @param clazz The class to create the JAXB context, marshaller and unmarshaller for.
     *
     * @throws JAXBException An error occurred while creating the JAXB context.
     */
    public JaxbContext(final Class<?> clazz) throws JAXBException {
      this.jaxbContextObj = JAXBContext.newInstance(clazz);
      this.marshaller = jaxbContextObj.createMarshaller();
      this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      this.unmarshaller = jaxbContextObj.createUnmarshaller();
    }

    /**
     * Unmarshalls the specified Input Stream. That is, converts XML data into a java content
     * tree.
     *
     * @param fileStream The {@link InputStream} to unmarshall.
     *
     * @return The unmarshalled object.
     *
     * @throws JAXBException An error occurred while unmarshalling the Input Stream.
     */
    public Object unmarshall(final InputStream fileStream) throws JAXBException {
      return unmarshaller.unmarshal(fileStream);
    }

    /**
     * Returns the {@code Marshaller} object that can be used to convert a java content tree
     * into XML data.
     *
     * @return The {@code Marshaller} object.
     */
    public Marshaller getMarshaller() {
      return this.marshaller;
    }

  }

}
