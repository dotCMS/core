/**
 * 
 */
package com.dotmarketing.business;

import java.util.List;

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
	public Layout findLayout(String layoutId) throws DotDataException;
	
	/**
	 * tries to cache first, then db to find layout
	 * @param layoutId
	 * @return
	 * @throws DotDataException
	 */
	public Layout loadLayout(String layoutId) throws DotDataException;
	
	/**
	 * 
	 * @param layout
	 * @throws DotDataException
	 */
	public void saveLayout(Layout layout) throws LayoutNameAlreadyExistsException, DotDataException;
	
	/**
	 * 
	 * @param layout
	 * @throws DotDataException
	 */
	public void removeLayout(Layout layout) throws DotDataException;
	
	/**
	 * Will reset all portlets on the layout to the passed in portlets to the order 
	 * they are in the list.
	 * @param layout
	 */
	public void setPortletsToLayout(Layout layout, List<Portlet> portlets) throws DotDataException;

	/**
	 * Will reset all portlets on the layout to the passed in portlets to the order 
	 * they are in the list.
	 * @param layout
	 */
	public void setPortletIdsToLayout(Layout layout, List<String> portletIds) throws DotDataException;

	/**
	 * Get all the layouts in order for a User.
	 * @return
	 */
	public List<Layout> loadLayoutsForUser(User user) throws DotDataException;
	
	/**
	 * Get all the layouts in order for a Role.
	 * @return
	 * @author David H Torres
	 */
	public List<Layout> loadLayoutsForRole(Role role) throws DotDataException;

	public boolean doesUserHaveAccessToPortlet(String portletId, User user) throws DotDataException;
	
	/**
	 * Retrieves all layouts in the system
	 * @author David H Torres
	 */
	public List<Layout> findAllLayouts() throws DotDataException;
	
}
