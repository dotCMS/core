/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.liferay.portal.ejb;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.util.Logger;
import com.liferay.portal.NoSuchPortletException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.auth.PrincipalException;
import com.liferay.portal.model.Portlet;
import com.liferay.util.FileUtil;

/**
 * <a href="PortletManagerImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public class PortletManagerImpl extends PrincipalBean implements PortletManager {

  private static final String _SHARED_KEY = "SHARED_KEY";

  private Map<String, Portlet> portletsXMLtoPortlets(final String pathToXmlFile) throws IOException, JDOMException {

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
      Portlet portlet = portletXMLtoPortlet(portletXML);
      portlets.put(portlet.getPortletId(), portlet);
    }

    return portlets;
  }

  private Portlet portletXMLtoPortlet(String xml) throws IOException, JDOMException {
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
        portlets.putAll(portletsXMLtoPortlets(xml));
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

    final List<Portlet> ports = PortletUtil.findAllDb();

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

  @Override
  public Portlet updatePortlet(final String portletId, String groupId, final String defaultPreferences, final boolean narrow,
      final String roles, final boolean active) throws PortalException, SystemException {

    // PortletPreferencesManagerUtil.deleteAllByGroup(groupId);

    final String companyId = getUser().getCompanyId();

    if (!hasAdministrator(companyId)) {
      throw new PrincipalException();
    }

    groupId = _SHARED_KEY;

    final PortletPK pk = new PortletPK(portletId, groupId, companyId);

    Portlet portlet = null;

    try {
      portlet = PortletUtil.findByPrimaryKey(pk);
    } catch (final NoSuchPortletException nspe) {
      portlet = PortletUtil.create(pk);
    }

    portlet.setDefaultPreferences(defaultPreferences);
    portlet.setNarrow(narrow);
    portlet.setRoles(roles);
    portlet.setActive(active);

    PortletUtil.update(portlet);

    portlet = getPortletById(portletId);

    portlet.setDefaultPreferences(defaultPreferences);
    portlet.setNarrow(narrow);
    portlet.setRoles(roles);
    portlet.setActive(active);
    portlet.setResourceBundle("com.liferay.portlet.StrutsResourceBundle");
    return portlet;
  }

}
