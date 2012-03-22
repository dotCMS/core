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

	protected abstract void save(HostVariable object) throws DotDataException ;
	/**
	 * Retrieves the list of all Host Variables
	 * 
	 * @param 
	 * @return
	 * @throws DotDataException
	 */
	
	protected abstract  List <HostVariable> getAllVariables() throws DotDataException ;

	/**
	 * Retrieves the list of  Host Variables associated
	 * to the given id.
	 * 
	 * @param id
	 * @return
	 * @throws DotDataException
	 */
	
	protected abstract List<HostVariable> getVariablesForHost (String hostId ) throws DotDataException ;

}
