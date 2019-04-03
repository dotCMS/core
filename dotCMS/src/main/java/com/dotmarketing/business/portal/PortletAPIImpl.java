package com.dotmarketing.business.portal;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.ServletContext;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.PortletContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Layout;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.portal.servlet.PortletContextPool;
import com.liferay.portal.servlet.PortletContextWrapper;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portlet.PortletConfigImpl;
import com.liferay.portlet.PortletContextImpl;
import com.liferay.util.CollectionFactory;
import com.liferay.util.SimpleCachePool;

public class PortletAPIImpl implements PortletAPI {

  String companyId = CompanyUtils.getDefaultCompany().getCompanyId();

  private ServletContext context;

  public PortletAPIImpl(PortletFactory portletFac, ServletContext context) {
    this.portletFac = portletFac;
    this.context = context;
  }

  public PortletAPIImpl() {
    this(new PortletFactoryImpl(), Config.CONTEXT);
  }

  final PortletFactory portletFac;

  protected boolean hasPortletRights(User user, String pId) {
    boolean hasRights = false;
    try {
      for (Layout layout : APILocator.getLayoutAPI().loadLayoutsForUser(user)) {
        if (layout.getPortletIds().contains(pId)) {
          return true;
        }
      }
    } catch (Exception ex) {
      Logger.warn(this, "can't determine if user " + user.getUserId() + " has rights to portlet " + pId, ex);
      hasRights = false;
    }
    return hasRights;
  }

  public boolean hasUserAdminRights(User user) {
    return hasPortletRights(user, "users");
  }

  public boolean hasContainerManagerRights(User user) {
    return hasPortletRights(user, "containers");
  }

  public boolean hasTemplateManagerRights(User user) {
    return hasPortletRights(user, "templates");
  }
  @Override
  @CloseDBIfOpened
  public Portlet findPortlet(String portletId) {
    if(portletId==null) {
      return null;
    }
    return portletFac.findById(portletId);

  }

  @Override
  @CloseDBIfOpened
  public void deletePortlet(String portletId) {

    try {
      portletFac.deletePortlet(portletId);
    } catch (Exception e) {
      throw new DotRuntimeException(e);
    }
  }

  @Override
  @CloseDBIfOpened
  public Portlet savePortlet(final Portlet portlet) {

    if(!UtilMethods.isSet(portlet.getPortletId())) {
      throw new DotStateException("You cannot save a portlet without an ID");
    }
    if(!UtilMethods.isSet(portlet.getPortletClass())) {
      throw new DotStateException("You cannot save a portlet without an implementing portletClass");
    }

    try {
      return portletFac.insertPortlet(portlet);
    } catch (Exception e) {
      throw new DotRuntimeException(e);
    }
  }
  
  
  
  @CloseDBIfOpened
  public void InitPortlets() throws SystemException {
    portletFac.getPortlets();
  }
  @Override
  @CloseDBIfOpened
  public Collection<Portlet> findAllPortlets() throws SystemException {
    return portletFac.getPortlets();
  }
  @Override
  public boolean canAddPortletToLayout(Portlet portlet) {
    String[] portlets = PropsUtil.getArray(PropsUtil.PORTLETS_EXCLUDED_FOR_LAYOUT);
    for (String portletId : portlets) {
      if (portletId.trim().equals(portlet.getPortletId()))
        return false;
    }
    return true;
  }
  @Override
  public boolean canAddPortletToLayout(String portletId) {
    String[] attachablePortlets = PropsUtil.getArray(PropsUtil.PORTLETS_EXCLUDED_FOR_LAYOUT);
    for (String attachablePortlet : attachablePortlets) {
      if (attachablePortlet.trim().equals(portletId))
        return false;
    }
    return true;
  }

  @Override
  public com.dotcms.repackage.javax.portlet.Portlet getImplementingInstance(final Portlet portlet) {

    if (portlet.getCachedInstance().isPresent()) {
      return portlet.getCachedInstance().get();
    } 


    PortletConfig config = getPortletConfig(portlet);

    return portlet.getCachedInstance(config);
  }

  @Override
  public PortletConfig getPortletConfig(Portlet portlet) {
    return new PortletConfigImpl(portlet.getPortletId(), getPortletContext(), portlet.getInitParams(), portlet.getResourceBundle());
  }

  @Override
  public PortletContext getPortletContext() {

    return new PortletContextImpl(context);
  }

}
