package com.dotmarketing.business;

import java.util.List;

import com.dotmarketing.exception.DotDataException;

/**
 * 
 * @author Jason Tesser
 *
 */
public abstract class LayoutFactory {

	/**
	 * tris to load layout from cache, then if not there, goes to db
	 * @param layoutId
	 * @return
	 * @throws DotDataException
	 */
	protected abstract Layout loadLayout(String layoutId) throws DotDataException;
	
	/**
	 * goes directlyto the db to find a layout
	 * @param layoutId
	 * @return
	 * @throws DotDataException
	 */
	protected abstract Layout findLayout(String layoutId) throws DotDataException;
	
	/**
	 * 
	 * @param layout
	 * @throws DotDataException
	 */
	protected abstract void saveLayout(Layout layout) throws DotDataException;
	
	/**
	 * 
	 * @param layout
	 * @throws DotDataException
	 */
	protected abstract void removeLayout(Layout layout) throws DotDataException;
	
	/**
	 * Will reset all portlets on the layout to the passed in portlets to the order 
	 * they are in the list.
	 * @param layout
	 */
	protected abstract void setPortletsToLayout(Layout layout, List<String> portletIds) throws DotDataException;

	/**
	 * Retrieves all layouts in the system
	 * @return
	 * @author David H Torres
	 * @throws DotDataException 
	 */
	protected abstract  List<Layout> findAllLayouts() throws DotDataException;

	protected abstract  Layout findLayoutByName(String name) throws DotDataException;

}
