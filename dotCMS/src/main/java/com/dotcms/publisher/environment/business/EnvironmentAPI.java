package com.dotcms.publisher.environment.business;

import java.util.List;
import java.util.Set;

import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

public interface EnvironmentAPI {

	/**
	 * Returns an Environment object whose id matches the given id, null if no match is found
	 *
	 * @param	id	the id of the environment object to get
	 * @return		the environment with the specified id
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public Environment findEnvironmentById(String id) throws DotDataException;

	/**
	 * Returns an Environment object whose name matches the given name, null if no match is found
	 *
	 * @param	name	the name of the environment object to get
	 * @return			the environment with the specified name
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public Environment findEnvironmentByName(String name) throws DotDataException;

	/**
	 * Returns a List of Environment objects that can be used by the Role with the given roleId,
	 * empty List if no match is found
	 *
	 * @param	roleId	the id of the role whose environments are requested
	 * @return			the list of environments related to the given roleId
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public Set<Environment> findEnvironmentsByRole(String roleId) throws DotDataException, NoSuchUserException, DotSecurityException;

	/**
	 * persists the given Environment object, and its permissions, to the underlying data layer
	 *
	 * @param	environment	the environment to be persisted
	 * @param	perms		a list of the environment's permissions to be persisted
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public void saveEnvironment(Environment environment, List<Permission> perms) throws DotDataException, DotSecurityException;

	/**
	 * Returns a List of all Environment objects, empty list if not environments are found
	 *
	 * @return			the list of environments
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public List<Environment> findAllEnvironments() throws DotDataException;

	/**
	 * Returns a List of all Environment objects which have Servers (or Endpoints) in it, empty list if not environments are found
	 *
	 * @return			the list of environments
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public List<Environment> findEnvironmentsWithServers() throws DotDataException;

	/**
	 * deletes the Environment object with the given id and its permissions from the underlying data layer
	 *
	 * @param	id	the id of the environment to be deleted
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public void deleteEnvironment(String id) throws DotDataException;

	/**
	 * updates the given Environment object in the underlying data layer
	 *
	 * @param	environment	the environment to be updated
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	void updateEnvironment(Environment environment, List<Permission> perms) throws DotDataException, DotSecurityException;

	/**
	 * Returns a List of Environment objects to whom the bundle with the given bundleId was sent to,
	 * empty list if not match is found
	 *
	 * @param	bundleId	the id of the bundle whose destination environments wants to be retrieved
	 * @return				a list of environments
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public List<Environment> findEnvironmentsByBundleId(String bundleId) throws DotDataException;

}
