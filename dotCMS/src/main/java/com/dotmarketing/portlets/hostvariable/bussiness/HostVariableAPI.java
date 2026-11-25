package com.dotmarketing.portlets.hostvariable.bussiness;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.model.User;
import java.util.List;

public interface HostVariableAPI {
	
	void delete(HostVariable object, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException ;

	HostVariable find(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException ;
	
	List<HostVariable > getAllVariables(String hostId,User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	List<HostVariable > getVariablesForHost(String hostId,User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException; 
	
	/**
	 * Saves a Site Variable on the Site it names ({@link HostVariable#getHostId()}). The user needs EDIT permission on
	 * that Site. Without an id the variable is created; with an id it must already be one of that Site's variables.
	 *
	 * @param object               The variable to save.
	 * @param user                 The user performing the save operation.
	 * @param respectFrontendRoles A flag indicating whether or not to respect frontend roles.
	 * @throws DotDataException     The id is not one of the Site's variables, or the data layer failed.
	 * @throws DotSecurityException The user cannot edit the Site.
	 */
	void save( HostVariable object, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Saves a list of site variables for a specific site. Each variable is matched to an existing variable of the site
	 * by id, then by key; a key match updates that variable and ignores the incoming id. A variable without a match is
	 * created on the site, and is refused if it carries an id that is not one of the site's variables. Existing
	 * variables of the site that are not in the list are deleted.
	 *
	 * @param siteVariables        The list of site variables to be saved.
	 * @param siteId               The ID of the site where the variables are being saved.
	 * @param user                 The user performing the save operation.
	 * @param respectFrontendRoles A flag indicating whether or not to respect frontend roles for
	 *                             the save operation.
	 * @return The list of saved site variables.
	 * @throws DotDataException     If there is an error accessing the data layer.
	 * @throws DotSecurityException If there is a security violation.
	 * @throws LanguageException    If there is an error accessing the language layer.
	 */
	List<HostVariable> save(List<HostVariable> siteVariables, String siteId, User user,
			boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException, LanguageException;
	
	HostVariable copy (HostVariable sourceVariable, Host destinationHost, User user, boolean respectFrontendRoles) 
		throws DotDataException, DotSecurityException;

	/**
	 * Updates the host_variable table when a user is going to be deleted.
	 * @param userToDelete UserId of the user that is going to be deleted
	 * @param userToReplace UserId of the user that is going to be replace it
	 * @throws DotDataException
	 */
    void updateUserReferences(String userToDelete, String userToReplace) throws DotDataException;

}
