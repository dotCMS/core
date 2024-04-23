package com.dotmarketing.portlets.hostvariable.bussiness;


import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;


public abstract  class HostVariableFactory {
	
	/**
	 * Totally removes a  Host Variable  from the system
	 * @param object
	 * @throws DotDataException
	 */
	
	protected abstract  void delete(HostVariable object) throws DotDataException ;

	/**
	 * Deletes all site variables associated with a specific site.
	 *
	 * @param siteId the ID of the site
	 * @throws DotDataException if an error occurs while deleting the variables
	 */
	public abstract void deleteAllVariablesForSite(String siteId) throws DotDataException;
	
	/**
	 * This method get a  Host Variable  object from the cache based
	 * on the passed id, if the object does not exist
	 * in cache a null value is returned
	 * @param id
	 * @return 
	 * @throws DotDataException
	 */
	
	protected abstract  HostVariable find (String id) throws DotDataException ;

	/**
	 * This method saves a Host Variable in the system
	 * @param object
	 * @throws DotDataException
	 */

	protected abstract HostVariable save(HostVariable object) throws DotDataException ;
	/**
	 * Retrieves the list of all Host Variables
	 * 
	 * @param 
	 * @return
	 * @throws DotDataException
	 */

	protected abstract List<HostVariable> getAllVariables() throws DotDataException;

	/**
	 * Retrieves the list of Site Variables associated to the given id.
	 *
	 * @param siteId The ID of the site whose variables will be retrieved.
	 * @return The list of Site Variables
	 * @throws DotDataException
	 */
	public abstract List<HostVariable> getVariablesForHost(String siteId)
			throws DotDataException;

	protected abstract void updateUserReferences(String userToDelete, String userToReplace) throws DotDataException ;

}
