/**
 * 
 */
package com.dotmarketing.business;

import java.util.List;
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

	/**
	 * returns or create the getting started layout
	 * @return
	 */
    Layout findGettingStartedLayout();

    /**
     * Adds a layout to a user (using the user's role)
     * @param layout layout to be added
     * @param user user that the layout will be added
     */
    void addLayoutForUser(Layout layout, User user) throws DotDataException;
	
	
	
}
