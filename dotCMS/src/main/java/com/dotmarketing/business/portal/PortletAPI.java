package com.dotmarketing.business.portal;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
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

    /**
     * This method is encapsulating the logic to create or update a portlet
     *
     * @param portlet the portlet to be created or updated
     * @param user the user that is creating or updating the portlet
     */
  Portlet savePortlet(final Portlet portlet, final User user) throws DotDataException, LanguageException;

  Portlet updatePortlet(final Portlet portlet) throws DotDataException;

    /**
     * This method is used to remove the prefix from the portletId.
     * We are avoiding to add double prefix to the portletId,
     * and also to avoid to have a portletId with a prefix that is not the expected one.
     *
     * @param	portletId the portlet id to be cleaned
     */
    String portletIdPrefixCleaner(final String portletId) throws DotDataValidationException;

}
