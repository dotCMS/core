/**
 * 
 */
package com.dotmarketing.business;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;

/**
 * 
 * @author Jason Tesser
 *
 */
public interface LayoutAPI {

	/**
	 * Goes direct to db to find layout
	 * @param layoutId
	 * @return
	 * @throws DotDataException
	 */
	Layout findLayout(String layoutId) throws DotDataException;
	
	/**
	 * tries to cache first, then db to find layout
	 * @param layoutId
	 * @return
	 * @throws DotDataException
	 */
	Layout loadLayout(String layoutId) throws DotDataException;
	
	/**
	 * 
	 * @param layout
	 * @throws DotDataException
	 */
	void saveLayout(Layout layout) throws LayoutNameAlreadyExistsException, DotDataException;
	
	/**
	 * 
	 * @param layout
	 * @throws DotDataException
	 */
	void removeLayout(Layout layout) throws DotDataException;
	
	/**
	 * Will reset all portlets on the layout to the passed in portlets to the order 
	 * they are in the list.
	 * @param layout
	 */
	void setPortletsToLayout(Layout layout, List<Portlet> portlets) throws DotDataException;

	/**
	 * Will reset all portlets on the layout to the passed in portlets to the order 
	 * they are in the list.
	 * @param layout
	 */
	void setPortletIdsToLayout(Layout layout, List<String> portletIds) throws DotDataException;

	/**
	 * Get all the layouts in order for a User.
	 * @return
	 */
	List<Layout> loadLayoutsForUser(User user) throws DotDataException;
	
	/**
	 * Get all the layouts in order for a Role.
	 * @return
	 * @author David H Torres
	 */
	List<Layout> loadLayoutsForRole(Role role) throws DotDataException;

	/**
	 * Returns true if the user has access to the portletId
	 * @param portletId {@link String}
	 * @param user      {@link User}
	 * @return boolean
	 * @throws DotDataException
	 */
	boolean doesUserHaveAccessToPortlet(String portletId, User user) throws DotDataException;
	
	/**
	 * Retrieves all layouts in the system
	 * @author David H Torres
	 */
	List<Layout> findAllLayouts() throws DotDataException;

	/**
	 * Find layout by name
	 *
	 * @param name
	 * @return
	 * @throws DotDataException throw when a error occur
	 */
	Layout findLayoutByName(String name) throws DotDataException;

	/**
	 * will return the requests layout or if not present, the layout from the
	 * referer
	 * @param request
	 * @return
	 */
    Optional<Layout> resolveLayout(HttpServletRequest request);


	/**
	 * Try to find the layout associated to a role
	 * @param layout {@link Layout}
	 * @param role   {@link Role}
	 * @return Optional LayoutsRoles, present and LayoutsRoles identifier set if exists.
	 */
	Optional<LayoutsRoles> findLayoutByRole(final Layout layout, final Role role);

	String GETTING_STARTED_LAYOUT_ID="2df9f117-b140-44bf-93d7-5b10a36fb7f9";

	String GETTING_STARTED_LAYOUT_NAME = "Getting Started";

	/**
	 * Resolves the product's Getting Started section: by {@link #GETTING_STARTED_LAYOUT_ID}; if no
	 * section has that id, by {@link #GETTING_STARTED_LAYOUT_NAME}, adopting that section as it is;
	 * and only when neither exists, creates it with its default name, icon, position and welcome
	 * tool. A resolved section that holds no tools gets the welcome tool back. The name, icon and
	 * position of an existing section are never rewritten.
	 *
	 * @return the Getting Started section, with its tools populated
	 */
    Layout findGettingStartedLayout();

	/**
	 * Rewrites the navigation position of several sections in one transaction: either every
	 * position is written or none is. Evicts each section from the layout cache and notifies open
	 * sessions once.
	 *
	 * @param tabOrderByLayoutId the new position of each section, keyed by section id; every id
	 *                           must name an existing section
	 * @throws DotDataException if an id names no section or the write fails; nothing is written
	 */
	default void setTabOrders(final Map<String, Integer> tabOrderByLayoutId) throws DotDataException {
		// Default so implementations outside core (OSGi plugins) keep compiling; core overrides it.
		throw new UnsupportedOperationException("setTabOrders is not supported by " + getClass().getName());
	}

    /**
     * Adds a layout to a user (using the user's role)
     * @param layout layout to be added
     * @param user user that the layout will be added
     */
    void addLayoutForUser(Layout layout, User user) throws DotDataException;
	
	
	
}
