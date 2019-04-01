
package com.dotmarketing.business.portal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.google.common.collect.Lists;
import com.liferay.portal.NoSuchPortletException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.auth.PrincipalException;
import com.liferay.portal.ejb.PortletManager;
import com.liferay.portal.ejb.PortletUtil;
import com.liferay.portal.ejb.PrincipalBean;
import com.liferay.portal.model.Portlet;
import com.liferay.util.FileUtil;

import io.vavr.control.Try;


public class PortletFactory extends PrincipalBean implements PortletManager {

  private static final String _SHARED_KEY = "SHARED_KEY";

  private Map<String, Portlet> xmlToPortlets(final String pathToXmlFile) throws IOException, JDOMException {

    final Map<String, Portlet> portlets = new HashMap<>();

    if (pathToXmlFile == null) {
      return portlets;
    }

    SAXBuilder builder = new SAXBuilder();
    // reader.setEntityResolver(resolver);
    Document doc = (Document) builder.build(pathToXmlFile);

    List<Element> list = doc.getRootElement().getChildren("portlet");

    for (Element node : list) {
      String portletXML = new XMLOutputter(Format.getCompactFormat()).outputString(node);
      Portlet portlet = xmlToPortlet(portletXML);
      portlets.put(portlet.getPortletId(), portlet);
    }

    return portlets;
  }

  private Portlet xmlToPortlet(String xml) throws IOException, JDOMException {
    InputStream stream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
    SAXBuilder builder = new SAXBuilder();
    Document doc = (Document) builder.build(stream);
    final Element node = doc.getRootElement();
    final String portletId = node.getChildText("portlet-name");
    String portletClass = node.getChildText("portlet-class");
    final Portlet portlet = new Portlet(portletId, portletClass, _SHARED_KEY, xml, false, null, true);
    portlet.setPortletClass(portletClass);
    portlet.setDefaultPreferences(xml);
    Map<String, String> params = new HashMap<>();
    for (Object n : node.getChildren("init-param")) {
      final Element initParam = (Element) n;
      params.put(initParam.getChildText("name"), initParam.getChildText("value"));
    }
    portlet.setInitParams(params);
    portlet.setResourceBundle(node.getChildText("resource-bundle"));
    return portlet;
  }

  @Override
  public Map<String, Portlet> addPortlets(final String[] xmlFiles) throws com.liferay.portal.SystemException {
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

  @WrapInTransaction
  @Override
  public void deletePortlet(final String portletId) throws SystemException, PortalException {
    try {
      final DotConnect db = new DotConnect();

      db.setSQL("delete from cms_layouts_portlets where portlet_id=?").addParam(portletId).loadResult();
      db.setSQL("delete from portletpreferences where portletid=?").addParam(portletId).loadResult();
      db.setSQL("delete from portlet where portletid=?").addParam(portletId).loadResult();


    } catch (final Exception e) {
      throw new SystemException(e);
    }
  }

  @Override
  public Portlet getPortletById(final String portletId) throws SystemException {

    return getPortletMap().get(portletId);
  }

  private synchronized final Map<String, Portlet> loadSystemPortlets() throws SystemException {
    final Map<String, Portlet> portlets = new HashMap<>();

    try {
      final String[] xmls = new String[] {FileUtil.getRealPath("/WEB-INF/portlet.xml"), FileUtil.getRealPath("/WEB-INF/portlet-ext.xml"),
          // FileUtil.getRealPath("/WEB-INF/liferay-portlet.xml"),
          // FileUtil.getRealPath("/WEB-INF/liferay-portlet-ext.xml")
      };

      portlets.putAll(addPortlets(xmls));
    } catch (final Exception e) {
      Logger.error(this, e.getMessage(), e);
    }

    final List<Portlet> ports = Try.of(()->findAllDb()).getOrElse(Lists.newArrayList());

    for (Portlet p : ports) {
      portlets.put(p.getPortletId(), (Portlet) p.clone());
    }
    return portlets;

  }

  private Map<String, Portlet> getPortletMap() throws SystemException {

    final PortletCache cache = new PortletCache();

    Map<String, Portlet> portletsMap = cache.getAllPortlets();

    if (portletsMap.isEmpty()) {

      portletsMap.putAll(loadSystemPortlets());
      cache.putAllPortlets(portletsMap);
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

      Portlet testPortlet = new Portlet(
          (String) map.get("portletid"), 
          (String) map.get("groupid"), 
          (String) map.get("companyid"),
          (String) map.get("defaultpreferences"), 
          false, 
          (String) map.get("roles"), 
          true);

      try {
        Portlet xmlPortlet = xmlToPortlet((String) map.get("defaultpreferences"));
        portlets.add(xmlPortlet);
      } catch (Exception e) {
        Logger.warn(this.getClass(), "unable to parse portlet from xml:" + testPortlet.getPortletId() + " " + e);
      }

    }
    return portlets;
  }
  
  
  
  
  @Override
  public Portlet updatePortlet(final String portletId, final String groupId, final String defaultPreferences, final boolean narrow,
      final String roles, final boolean active) throws PortalException, SystemException {

    // PortletPreferencesManagerUtil.deleteAllByGroup(groupId);

    final String companyId = getUser().getCompanyId();

    if (!hasAdministrator(companyId)) {
      throw new PrincipalException();
    }

    


    Optional<Portlet> portletOpt = findById(portletId);
    if(portletOpt.isPresent()) {
      Portlet portlet = portletOpt.get();
      portlet.setDefaultPreferences(defaultPreferences);
      portlet.setNarrow(narrow);
      portlet.setRoles(roles);
      portlet.setActive(active);
  
      PortletUtil.update(portlet);
    }
    
    
    
    portlet = getPortletById(portletId);

    portlet.setDefaultPreferences(defaultPreferences);
    portlet.setNarrow(narrow);
    portlet.setRoles(roles);
    portlet.setActive(active);
    portlet.setResourceBundle("com.liferay.portlet.StrutsResourceBundle");
    return portlet;
  }
  
  
  @Override
  public Optional<Portlet> findById(final String portletId) throws DotDataException {

    DotConnect db = new DotConnect();
    db.setSQL("select * from portlet where companyid=? and portletid=?").addParam("dotcms.org").addParam(portletId);

    final List<Map<String, Object>> maps = db.loadObjectResults();
    for (Map<String, Object> map : maps) {
      return Optional.ofNullable(fromMap(map)); 
    }
    return Optional.empty();
  }
  
  
  private Portlet fromMap(Map<String,Object> map) {
    Portlet testPortlet = new Portlet(
        (String) map.get("portletid"), 
        (String) map.get("groupid"), 
        (String) map.get("companyid"),
        (String) map.get("defaultpreferences"), 
        false, 
        (String) map.get("roles"), 
        true);
    try {
      return xmlToPortlet((String) map.get("defaultpreferences"));

    } catch (Exception e) {
      Logger.warn(this.getClass(), "unable to parse portlet from xml:" + testPortlet.getPortletId() + " " + e);
    }
    return testPortlet;
    
  }
  
  
  @WrapInTransaction
  @Override
  public Portlet savePortlet(final Portlet portlet) throws DotDataException {
    final String portletXML = portletToXml(portlet);
    
    DotConnect db = new DotConnect();
    
    db.setSQL("delete from portlet where portletid=?").addParam(portlet.getPortletId()).loadResult();
    db.setSQL("insert into portlet (portletid, groupid, companyid, defaultpreferences, narrow, roles, active_) values(?,?,?,?,false,?, true")
    .addParam(portlet.getPortletId())
    .addParam(portlet.getGroupId())
    .addParam(portlet.getCompanyId())
    .addParam(portletXML)
    .addParam(portlet.getRoles())
    .loadResult();
    return portlet;

  }
  

  /**
   * 
    <portlet>
        <portlet-name>html-pages</portlet-name>
        <display-name>HTML Pages Manager</display-name>
        <portlet-class>com.liferay.portlet.StrutsPortlet</portlet-class>
        <init-param>
            <name>view-action</name>
            <value>/ext/htmlpages/view_htmlpages</value>
        </init-param>


        <resource-bundle>com.liferay.portlet.StrutsResourceBundle</resource-bundle>


    </portlet>
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
    
    child = new Element("init-params");
    Map<String,String> params = portlet.getInitParams();
    for(Entry<String,String> entry: params.entrySet()) {
      Element grandChild = new Element("name");
      grandChild.addContent(entry.getKey());
      child.addContent(grandChild);
      
      grandChild = new Element("value");
      grandChild.addContent(entry.getValue());
      child.addContent(grandChild);

    }
    root.addContent(child);
    doc.setRootElement(root);
    
    return new XMLOutputter().outputString(doc); 
    



  }
  
  
}
