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

	/** Init param that records where a portlet definition was registered from. */
	String PORTLET_SOURCE_INIT_PARAM = "portletSource";

	/**
	 * Value of {@link #PORTLET_SOURCE_INIT_PARAM} written by {@link #savePortlet(Portlet, User)}
	 * for every custom content tool it stores in the database. Portlets parsed from the XML files
	 * carry no marker and default to {@code xml}.
	 */
	String DB_PORTLET_SOURCE = "db";

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

    /**
     * Tells whether the given portlet is a custom content tool, that is, one an admin created
     * through the custom-tool operations rather than one shipped with the product.
     * <p>
     * A portlet is custom when it was registered in the database (its
     * {@link #PORTLET_SOURCE_INIT_PARAM} init param equals {@link #DB_PORTLET_SOURCE}) <b>and</b>
     * its id is not one the product declares in {@link com.dotmarketing.util.PortletID}. The
     * conventional {@code c_} id prefix is deliberately not consulted, so a change to the id
     * scheme cannot mislabel tools. Portlets an OSGi plugin writes to the database keep their
     * XML origin and are therefore not custom either.
     *
     * @param portlet the portlet to classify; {@code null} is allowed
     * @return {@code true} only for an admin-made custom content tool
     */
    boolean isCustomContentPortlet(final Portlet portlet);

}
