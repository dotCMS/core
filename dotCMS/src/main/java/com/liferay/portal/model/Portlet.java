package com.liferay.portal.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.ejb.PortletPK;
import com.liferay.portlet.ConcretePortletWrapper;

public class Portlet extends PortletModel {

  private static final long serialVersionUID = 1L;
  private final String portletId;
  private final Map<String, String> initParams;
  private final String portletClass, portletSource;

  @Deprecated
  public Portlet(PortletPK pk) {
    super(pk);
    this.portletId = pk.portletId;
    setStrutsPath(pk.portletId);
    setActive(true);
    initParams = new HashMap<>();
    portletClass = null;
    portletSource="xml";

  }

  public Portlet(String portletId, String portletClass, Map<String, String> initParams) {
    super(portletId, "", "dotcms.org", null, false, null, true);
    this.portletId = portletId;
    this.initParams = initParams;
    this.portletClass = portletClass;
    this.portletSource=initParams.getOrDefault("portletSource", "xml");

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
    this.portletSource=initParams.getOrDefault("portletSource", "xml");
  }

  @Deprecated
  public Portlet(String portletId, String groupId, String companyId, String strutsPath, String portletClass, String indexerClass,
      String schedulerClass, String defaultPreferences, String prefsSharingType, boolean useDefaultTemplate,
      boolean showPortletAccessDenied, boolean showPortletInactive, boolean restoreCurrentView, boolean ns4Compatible, boolean narrow,
      String roles, boolean active, boolean include, Map<String,String> initParams, String resourceBundle, Set userAttributes, Map customUserAttributes,
      boolean warFile) {

    super(portletId, groupId, companyId, defaultPreferences, narrow, roles, active);
    this.portletId = portletId;
    this.initParams = initParams;
    this.portletClass = portletClass;
    this.portletSource=initParams.getOrDefault("portletSource", "xml");
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
    return "com.liferay.portlet.StrutsResourceBundle" ;
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

  private transient ConcretePortletWrapper cachedInstance = null;

  /**
   * Initialize the portlet instance.
   */

  public Optional<ConcretePortletWrapper> getCachedInstance() {
    return Optional.ofNullable(cachedInstance);

  }

  public ConcretePortletWrapper getCachedInstance(PortletConfig portletConfig) {

    try {
      if (cachedInstance == null) {
        com.dotcms.repackage.javax.portlet.Portlet realPortlet = (com.dotcms.repackage.javax.portlet.Portlet) Class.forName(getPortletClass()).newInstance();
        ConcretePortletWrapper newOne = new ConcretePortletWrapper(realPortlet, portletConfig.getPortletContext());
        newOne.init(portletConfig);
        cachedInstance = newOne;
      }

    } catch (Exception e) {
      Logger.error(this.getClass(), e.getMessage(), e);
    }

    return cachedInstance;
  }

}
