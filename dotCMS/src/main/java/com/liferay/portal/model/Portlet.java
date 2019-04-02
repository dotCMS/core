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
    setInclude(true);
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
        null, oldPort.getDefaultPreferences(), oldPort.getPreferencesValidator(), null, false, false, false, false, false, false,
        oldPort.getRoles(), true, false, oldPort.getInitParams(), oldPort.getExpCache(), oldPort.getPortletModes(),
        oldPort.getSupportedLocales(), oldPort.getResourceBundle(),  null, null, false);
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
      String schedulerClass, String defaultPreferences, String prefsValidator, String prefsSharingType, boolean useDefaultTemplate,
      boolean showPortletAccessDenied, boolean showPortletInactive, boolean restoreCurrentView, boolean ns4Compatible, boolean narrow,
      String roles, boolean active, boolean include, Map initParams, Integer expCache, Map portletModes, Set supportedLocales,
      String resourceBundle, Set userAttributes, Map customUserAttributes, boolean warFile) {

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
   * Gets the name of the indexer class of the portlet.
   *
   * @return the name of the indexer class of the portlet
   */
  public String getIndexerClass() {
    return null;
  }

  /**
   * Sets the name of the indexer class of the portlet.
   *
   * @param indexerClass the name of the indexer class of the portlet
   */
  public void setIndexerClass(String indexerClass) {

  }

  /**
   * Gets the name of the scheduler class of the portlet.
   *
   * @return the name of the scheduler class of the portlet
   */
  public String getSchedulerClass() {
    return null;
  }

  /**
   * Sets the name of the scheduler class of the portlet.
   *
   * @param schedulerClass the name of the scheduler class of the portlet
   */
  public void setSchedulerClass(String schedulerClass) {

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
   * Gets the name of the preferences validator class of the portlet.
   *
   * @return the name of the preferences validator class of the portlet
   */
  public String getPreferencesValidator() {
    return null;
  }

  /**
   * Sets the name of the preferences validator class of the portlet.
   *
   * @param prefsValidator the name of the preferences validator class of the portlet
   */
  public void setPreferencesValidator(String prefsValidator) {

  }

  /**
   * Returns the preferences sharing type of the portlet.
   *
   * @return the preferences sharing type of the portlet
   */
  public String getPreferencesSharingType() {
    return null;
  }

  /**
   * Sets the preferences sharing type of the portlet.
   *
   * @param prefsSharingType the preferences sharing type of the portlet
   */
  public void setPreferencesSharingType(String prefsSharingType) {

  }

  /**
   * Returns true if the portlet uses the default template.
   *
   * @return true if the portlet uses the default template
   */
  public boolean getUseDefaultTemplate() {
    return true;
  }

  /**
   * Returns true if the portlet uses the default template.
   *
   * @return true if the portlet uses the default template
   */
  public boolean isUseDefaultTemplate() {
    return true;
  }

  /**
   * Sets to true if the portlet uses the default template.
   *
   * @param useDefaultTemplate boolean value for whether the portlet uses the default template
   */
  public void setUseDefaultTemplate(boolean useDefaultTemplate) {

  }

  /**
   * Returns true if users are shown that they do not have access to the portlet.
   *
   * @return true if users are shown that they do not have access to the portlet
   */
  public boolean getShowPortletAccessDenied() {
    return true;
  }

  /**
   * Returns true if users are shown that they do not have access to the portlet.
   *
   * @return true if users are shown that they do not have access to the portlet
   */
  public boolean isShowPortletAccessDenied() {
    return true;
  }

  /**
   * Sets to true if users are shown that they do not have access to the portlet.
   *
   * @param showPortletAccessDenied boolean value for whether users are shown that they do not have
   *        access to the portlet
   */
  public void setShowPortletAccessDenied(boolean showPortletAccessDenied) {

  }

  /**
   * Returns true if users are shown that the portlet is inactive.
   *
   * @return true if users are shown that the portlet is inactive
   */
  public boolean getShowPortletInactive() {
    return true;
  }

  /**
   * Returns true if users are shown that the portlet is inactive.
   *
   * @return true if users are shown that the portlet is inactive
   */
  public boolean isShowPortletInactive() {
    return true;
  }

  /**
   * Sets to true if users are shown that the portlet is inactive.
   *
   * @param showPortletInactive boolean value for whether users are shown that the portlet is inactive
   */
  public void setShowPortletInactive(boolean showPortletInactive) {

  }

  /**
   * Returns true if the portlet restores to the current view from the maximized state.
   *
   * @return true if the portlet restores to the current view from the maximized state
   */
  public boolean getRestoreCurrentView() {
    return true;
  }

  /**
   * Returns true if the portlet restores to the current view from the maximized state.
   *
   * @return true if the portlet restores to the current view from the maximized state
   */
  public boolean isRestoreCurrentView() {
    return true;
  }

  /**
   * Sets to true if the portlet restores to the current view from the maximized state.
   *
   * @param restoreCurrentView boolean value for whether the portlet restores to the current view from
   *        the maximized state
   */
  public void setRestoreCurrentView(boolean restoreCurrentView) {

  }


  /**
   * Sets a string of ordered comma delimited portlet ids.
   *
   * @param roles a string of ordered comma delimited portlet ids
   */
  public void setRoles(String roles) {

  }

  /**
   * Gets an array of required roles of the portlet.
   *
   * @return an array of required roles of the portlet
   */
  public String[] getRolesArray() {
    return null;
  }

  /**
   * Sets an array of required roles of the portlet.
   *
   * @param rolesArray an array of required roles of the portlet
   */
  public void setRolesArray(String[] rolesArray) {

  }

  /**
   * Returns true if the portlet has a role with the specified name.
   *
   * @return true if the portlet has a role with the specified name
   */
  public boolean hasRoleWithName(String roleName) {

    return false;
  }

  /**
   * Returns true to include the portlet and make it available to be made active
   *
   * @return true to include the portlet and make it available to be made active
   */
  public boolean getInclude() {
    return false;
  }

  /**
   * Returns true to include the portlet and make it available to be made active
   *
   * @return true to include the portlet and make it available to be made active
   */
  public boolean isInclude() {
    return false;
  }

  /**
   * Sets to true to include the portlet and make it available to be made active
   *
   * @param include boolean value for whether to include the portlet and make it available to be made
   *        active
   */
  public void setInclude(boolean include) {

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
   * Gets expiration cache of the portlet.
   *
   * @return expiration cache of the portlet
   */
  public Integer getExpCache() {
    return 1000;
  }

  /**
   * Sets expiration cache of the portlet.
   *
   * @param expCache expiration cache of the portlet
   */
  public void setExpCache(Integer expCache) {

  }

  /**
   * Gets the portlet modes of the portlet.
   *
   * @return portlet modes of the portlet
   */
  public Map getPortletModes() {
    return null;
  }

  /**
   * Sets the portlet modes of the portlet.
   *
   * @param portletModes the portlet modes of the portlet
   */
  public void setPortletModes(Map portletModes) {

  }

  /**
   * Returns true if the portlet supports the specified mime type and portlet mode.
   *
   * @return true if the portlet supports the specified mime type and portlet mode
   */
  public boolean hasPortletMode(String mimeType, PortletMode portletMode) {
    return true;

  }

  /**
   * Gets the supported locales of the portlet.
   *
   * @return supported locales of the portlet
   */
  public Set getSupportedLocales() {
    return null;
  }

  /**
   * Sets the supported locales of the portlet.
   *
   * @param supportedLocales the supported locales of the portlet
   */
  public void setSupportedLocales(Set supportedLocales) {}

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
   * Returns true if the portlet is found in a WAR file.
   *
   * @return true if the portlet is found in a WAR file
   */
  public boolean isWARFile() {
    return false;
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
        CachePortlet newOne = new CachePortlet(realPortlet, portletConfig.getPortletContext(), getExpCache());

        newOne.init(portletConfig);
        cachedInstance = newOne;
      }

    } catch (Exception e) {
      Logger.error(this.getClass(), e.getMessage(), e);
    }

    return cachedInstance;
  }

}
