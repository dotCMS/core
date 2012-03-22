package com.dotmarketing.business.portal;

import java.util.List;

import com.liferay.portal.SystemException;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;

public interface PortletAPI {

	boolean hasContainerManagerRights(User user);

	boolean hasTemplateManagerRights(User user);
	
	Portlet findPortlet(String id);
	
	List<Portlet> findAllPortlets () throws SystemException;
	
	boolean canAddPortletToLayout(Portlet portlet);

	boolean canAddPortletToLayout(String portletId);

}
