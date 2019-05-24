package com.dotmarketing.business.portal;

import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.language.LanguageException;
import java.util.Collection;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.PortletContext;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;

public interface PortletAPI {

	//Prefix used for new custom portlets
	public final static String CONTENT_PORTLET_PREFIX="c_";

	boolean hasContainerManagerRights(final User user);

	boolean hasTemplateManagerRights(final User user);
	
	Portlet findPortlet(final String id);

	Collection<Portlet> findAllPortlets () throws SystemException;
	
	boolean canAddPortletToLayout(final Portlet portlet);

	boolean canAddPortletToLayout(final String portletId);
	
	boolean hasUserAdminRights(final User user);

  com.dotcms.repackage.javax.portlet.Portlet getImplementingInstance(final Portlet portlet);

  PortletConfig getPortletConfig(final Portlet portlet);

  PortletContext getPortletContext();

  void deletePortlet(final String portletId);

  Portlet savePortlet(final Portlet portlet, final User user) throws DotDataException, LanguageException;

  Portlet updatePortlet(final Portlet portlet) throws DotDataException;

}
