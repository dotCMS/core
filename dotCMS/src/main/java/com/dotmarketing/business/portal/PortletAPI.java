package com.dotmarketing.business.portal;

import com.liferay.portal.SystemException;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import java.util.List;

public interface PortletAPI {

  boolean hasContainerManagerRights(User user);

  boolean hasTemplateManagerRights(User user);

  Portlet findPortlet(String id);

  List<Portlet> findAllPortlets() throws SystemException;

  boolean canAddPortletToLayout(Portlet portlet);

  boolean canAddPortletToLayout(String portletId);

  boolean hasUserAdminRights(User user);
}
