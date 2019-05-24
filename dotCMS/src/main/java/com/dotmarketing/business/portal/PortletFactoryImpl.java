
package com.dotmarketing.business.portal;
import com.dotmarketing.util.UtilMethods;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import javax.ws.rs.core.Response;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.PrincipalBean;
import com.liferay.portal.model.Portlet;
import com.liferay.util.FileUtil;

import io.vavr.control.Try;
import org.xml.sax.InputSource;

public class PortletFactoryImpl extends PrincipalBean implements PortletFactory {

  public PortletFactoryImpl(String[] systemXmlFiles) {
    this.systemXmlFiles = systemXmlFiles;
  }

  public PortletFactoryImpl() {
    this(new String[] {FileUtil.getRealPath("/WEB-INF/portlet.xml"), FileUtil.getRealPath("/WEB-INF/portlet-ext.xml")});
  }

  private final String[] systemXmlFiles;

  private Map<String, Portlet> xmlToPortlets(final String pathToXmlFile) throws IOException, JDOMException {

    final Map<String, Portlet> portlets = new HashMap<>();

    if (pathToXmlFile == null) {
      return portlets;
    }
    InputStream stream = new FileInputStream(new File(pathToXmlFile));
    return xmlToPortlets(stream);
  }

  private Map<String, Portlet> xmlToPortlets(InputStream fileStream) throws IOException, JDOMException {

    final Map<String, Portlet> portlets = new HashMap<>();

    if (fileStream == null) {
      return portlets;
    }

    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(fileStream);

    List<Element> list = doc.getRootElement().getChildren("portlet");

    for (Element node : list) {
      String portletXML = new XMLOutputter(Format.getCompactFormat()).outputString(node);
      Optional<DotPortlet> portlet = xmlToPortlet(portletXML);
      if (portlet.isPresent()) {
        portlets.put(portlet.get().getPortletId(), portlet.get());
      }
    }

    return portlets;
  }

  @Override
  @VisibleForTesting
  public Optional<DotPortlet> xmlToPortlet(String xml) throws IOException, JDOMException {
    InputStream stream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
    SAXBuilder builder = new SAXBuilder();
    Document doc = (Document) builder.build(stream);
    final Element node = doc.getRootElement();
    final String portletId = node.getChildText("portlet-name");

    final String portletClass = node.getChildText("portlet-class");

    Map<String, String> params = new HashMap<>();
    for (Object n : node.getChildren("init-param")) {
      final Element initParam = (Element) n;
      params.put(initParam.getChildText("name"), initParam.getChildText("value"));
    }
    

    if (portletId == null || portletClass == null) {
      return Optional.empty();
    }

    return Optional.of(new DotPortlet(portletId, portletClass, params));

  }

  @Override
  public Map<String, Portlet> xmlToPortlets(final String[] xmlFiles) throws com.liferay.portal.SystemException {
    final Map<String, Portlet> portlets = new HashMap<>();
    for (final String xml : xmlFiles) {
      try {
        portlets.putAll(xmlToPortlets(xml));
      } catch (final Exception e) {
        throw new SystemException(e);
      }
    }
    return portlets;
  }

  @Override
  public Map<String, Portlet> xmlToPortlets(final InputStream[] xmlFiles) throws com.liferay.portal.SystemException {
    final Map<String, Portlet> portlets = new HashMap<>();
    for (final InputStream xml : xmlFiles) {
      try {
        portlets.putAll(xmlToPortlets(xml));
      } catch (final Exception e) {
        throw new SystemException(e);
      }
    }
    return portlets;
  }

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

  private final Map<String, Portlet> loadSystemPortlets() {
    final Map<String, Portlet> portlets = new HashMap<>();

    try {
      portlets.putAll(xmlToPortlets(systemXmlFiles));
    } catch (final Exception e) {
      Logger.error(this, e.getMessage(), e);
    }

    final List<Portlet> ports = Try.of(() -> findAllDb()).getOrElse(Lists.newArrayList());

    for (Portlet p : ports) {
      portlets.put(p.getPortletId(), (Portlet) p);
    }
    return portlets;

  }

  private Map<String, Portlet> getPortletMap() {

    final PortletCache cache = new PortletCache();

    Map<String, Portlet> portletsMap = cache.getAllPortlets();

    if (portletsMap.isEmpty()) {
      synchronized (PortletCache.class) {
        portletsMap = cache.getAllPortlets();
        if (portletsMap.isEmpty()) {
          Map<String, Portlet> x = loadSystemPortlets();
          portletsMap.putAll(x);
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
  @CloseDBIfOpened
  public List<Portlet> findAllDb() throws DotDataException {

    DotConnect db = new DotConnect();
    db.setSQL("select * from portlet where companyid=?").addParam("dotcms.org");
    final List<Portlet> portlets = new ArrayList<>();
    final List<Map<String, Object>> maps = db.loadObjectResults();
    for (Map<String, Object> map : maps) {

      Portlet testPortlet = new Portlet((String) map.get("portletid"), (String) map.get("groupid"), (String) map.get("companyid"),
          (String) map.get("defaultpreferences"), false, (String) map.get("roles"), true);

      try {
        Optional<DotPortlet> xmlPortlet = xmlToPortlet((String) map.get("defaultpreferences"));
        if (xmlPortlet.isPresent()) {
          portlets.add(xmlPortlet.get());
        }

      } catch (Exception e) {
        Logger.warn(this.getClass(), "unable to parse portlet from xml:" + testPortlet.getPortletId() + " " + e);
      }

    }
    return portlets;
  }
  
  private Optional<DotPortlet> fromMap(final Map<String, Object> map) {
    final Portlet portlet = new Portlet((String) map.get("portletid"), (String) map.get("groupid"), (String) map.get("companyid"),
        (String) map.get("defaultpreferences"), false, (String) map.get("roles"), true);
    try {
      return xmlToPortlet((String) map.get("defaultpreferences"));

    } catch (Exception e) {
      Logger.warn(this.getClass(), "unable to parse portlet from xml:" + portlet.getPortletId() + " " + e);
    }
    return Optional.empty();

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
    DotConnect db = new DotConnect();

    db.setSQL("update portlet set groupid=?, defaultpreferences=? where portletid=?").addParam(portlet.getGroupId()).addParam(portletXML)
        .addParam(portlet.getPortletId()).loadResult();
    new PortletCache().clear();
    return portlet;

  }

  private boolean doesPortletExistInDb(Portlet portlet) {

    return (new DotConnect().setSQL("select count(*) as test from portlet where portletid=?").addParam(portlet.getPortletId())
        .getInt("test") > 0);

  }

  /**
   * 
   * <portlet> <portlet-name>html-pages</portlet-name> <display-name>HTML Pages Manager</display-name>
   * <portlet-class>com.liferay.portlet.StrutsPortlet</portlet-class> <init-param>
   * <name>view-action</name> <value>/ext/htmlpages/view_htmlpages</value> </init-param>
   * 
   * 
   * <resource-bundle>com.liferay.portlet.StrutsResourceBundle</resource-bundle>
   * 
   * 
   * </portlet>
   * 
   * @param portlet
   * @return
   * @throws DotDataException
   */
  @Override
  public String portletToXml(final Portlet portlet) throws DotDataException {

    Document doc = new Document();
    Element root = new Element("portlet");
    Element child = new Element("portlet-name");
    child.addContent(portlet.getPortletId());
    root.addContent(child);

    child = new Element("portlet-class");
    child.addContent(portlet.getPortletClass());
    root.addContent(child);

    child = new Element("resource-bundle");
    child.addContent(portlet.getResourceBundle());
    root.addContent(child);

    
    Map<String, String> params = portlet.getInitParams();
    for (Entry<String, String> entry : params.entrySet()) {
      child = new Element("init-param");
      Element grandChild = new Element("name");
      grandChild.addContent(entry.getKey());
      child.addContent(grandChild);

      grandChild = new Element("value");
      grandChild.addContent(entry.getValue());
      child.addContent(grandChild);
      root.addContent(child);
    }
    doc.setRootElement(root);

    return new XMLOutputter().outputString(doc);

  }

}
