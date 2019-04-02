package com.dotmarketing.business.portal;

import java.util.Collection;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.PortletContext;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;

public interface PortletAPI {

	boolean hasContainerManagerRights(User user);

	boolean hasTemplateManagerRights(User user);
	
	Portlet findPortlet(String id);
	
	Collection<Portlet> findAllPortlets () throws SystemException;
	
	boolean canAddPortletToLayout(Portlet portlet);

	boolean canAddPortletToLayout(String portletId);
	
	boolean hasUserAdminRights(User user);

  com.dotcms.repackage.javax.portlet.Portlet getImplementingInstance(Portlet portlet);

  PortletConfig getPortletConfig(Portlet portlet);

  PortletContext getPortletContext();

  void deletePortlet(String portletId);

}
