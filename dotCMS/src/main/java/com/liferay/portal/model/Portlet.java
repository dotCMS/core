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

package com.liferay.portal.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.PortletMode;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.ejb.PortletPK;
import com.liferay.portlet.CachePortlet;

/**
 * <a href="Portlet.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.49 $
 *
 */
public class Portlet extends PortletModel {

  private final String portletId;
  private final Map<String, String> initParams;
  private final String portletClass;

  @Deprecated
  public Portlet(PortletPK pk) {
    super(pk);
    this.portletId = pk.portletId;
    setStrutsPath(pk.portletId);
    setActive(true);
    initParams = new HashMap<>();
    portletClass = null;

  }

  public Portlet(String portletId, String extendsPortletId, String portletClass, Map<String, String> initParams) {
    super(portletId, extendsPortletId, "dotcms.org", null, false, null, true);
    this.portletId = portletId;
    this.initParams = initParams;
    this.portletClass = portletClass;
  }

  @Deprecated
  public Portlet(Portlet oldPort) {
    this(oldPort.getPortletId(), oldPort.getGroupId(), oldPort.getCompanyId(), oldPort.getStrutsPath(), oldPort.getPortletClass(), null,
        null, oldPort.getDefaultPreferences(), null, false, false, false, false, false, false, oldPort.getRoles(), true, false,
        oldPort.getInitParams(), oldPort.getResourceBundle(), null, null, false);
  }

  @Deprecated
  public Portlet(String portletId, String groupId, String companyId, String defaultPreferences, boolean narrow, String roles,
      boolean active) {

    super(portletId, groupId, companyId, defaultPreferences, narrow, roles, active);
    this.portletId = portletId;
    this.initParams = new HashMap<>();
    this.portletClass = null;
  }

  @Deprecated
  public Portlet(String portletId, String groupId, String companyId, String strutsPath, String portletClass, String indexerClass,
      String schedulerClass, String defaultPreferences, String prefsSharingType, boolean useDefaultTemplate,
      boolean showPortletAccessDenied, boolean showPortletInactive, boolean restoreCurrentView, boolean ns4Compatible, boolean narrow,
      String roles, boolean active, boolean include, Map initParams, String resourceBundle, Set userAttributes, Map customUserAttributes,
      boolean warFile) {

    super(portletId, groupId, companyId, defaultPreferences, narrow, roles, active);
    this.portletId = portletId;
    this.initParams = initParams;
    this.portletClass = portletClass;

  }

  /**
   * Gets the struts path of the portlet.
   *
   * @return the struts path of the portlet
   */
  public String getStrutsPath() {
    return getPortletId();
  }

  @Deprecated
  public void setStrutsPath(String strutsPath) {

  }

  public String getExtendsPortlet() {
    return getGroupId();
  }

  /**
   * Gets the name of the portlet class of the portlet.
   *
   * @return the name of the portlet class of the portlet
   */
  public String getPortletClass() {
    return portletClass;
  }

  /**
   * Sets the name of the portlet class of the portlet.
   *
   * @param portletClass the name of the portlet class of the portlet
   */
  public void setPortletClass(String portletClass) {
    portletClass = portletClass;
  }

  /**
   * Gets the default preferences of the portlet.
   *
   * @return the default preferences of the portlet
   */
  public String getDefaultPreferences() {
    return super.getDefaultPreferences();

  }

  /**
   * Gets the init parameters of the portlet.
   *
   * @return init parameters of the portlet
   */
  public Map<String, String> getInitParams() {
    return this.initParams;
  }

  /**
   * Sets the init parameters of the portlet.
   *
   * @param initParams the init parameters of the portlet
   */
  public void setInitParams(Map initParams) {
    throw new DotStateException("not supported");
  }

  /**
   * Gets the resource bundle of the portlet.
   *
   * @return resource bundle of the portlet
   */
  public String getResourceBundle() {
    return "com.liferay.portlet.StrutsPortlet".equals(portletClass) ? "com.liferay.portlet.StrutsResourceBundle" : null;
  }

  @Override
  public String getPortletId() {
    return this.portletId;
  }

  /**
   * Compares this portlet to the specified object.
   *
   * @param obj the object to compare this portlet against
   * @return the value 0 if the argument portlet is equal to this portlet; a value less than -1 if
   *         this portlet is less than the portlet argument; and 1 if this portlet is greater than the
   *         portlet argument
   */
  public int compareTo(Object obj) {
    Portlet portlet = (Portlet) obj;

    return getPortletId().compareTo(portlet.getPortletId());
  }

  private transient CachePortlet cachedInstance = null;

  /**
   * Initialize the portlet instance.
   */

  public Optional<CachePortlet> getCachedInstance() {
    return Optional.ofNullable(cachedInstance);

  }

  public CachePortlet getCachedInstance(PortletConfig portletConfig) {

    try {
      if (cachedInstance == null) {
        com.dotcms.repackage.javax.portlet.Portlet realPortlet =
            (com.dotcms.repackage.javax.portlet.Portlet) Class.forName(getPortletClass()).newInstance();
        CachePortlet newOne = new CachePortlet(realPortlet, portletConfig.getPortletContext());

        newOne.init(portletConfig);
        cachedInstance = newOne;
      }

    } catch (Exception e) {
      Logger.error(this.getClass(), e.getMessage(), e);
    }

    return cachedInstance;
  }

}
