package com.dotmarketing.portlets.hostvariable.bussiness;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.rendering.velocity.services.SiteLoader;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class HostVariableAPIImpl implements HostVariableAPI {

	final HostVariableFactory hostVariableFactory;
	final PermissionAPI permissionAPI;
	final HostAPI hostAPI = APILocator.getHostAPI();
	
	public HostVariableAPIImpl() {
		hostVariableFactory = FactoryLocator.getHostVariableFactory();
		permissionAPI = APILocator.getPermissionAPI();
	}
	
	@WrapInTransaction
	public void delete(HostVariable object, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		Host host = hostAPI.find(object.getHostId(), user, respectFrontendRoles);
		if(!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to edit the host variable= " + object.getId());
		
		hostVariableFactory.delete(object);
        new SiteLoader().invalidate(host);
		
	}

	@CloseDBIfOpened
	public HostVariable find(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		
		HostVariable hvar = hostVariableFactory.find(id);
		Host host = hostAPI.find(hvar.getHostId(), user, respectFrontendRoles);
		if(!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_USE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to read the host variable = " + hvar.getId());
		return hvar;
		
	}
	
	@CloseDBIfOpened
	public List<HostVariable > getAllVariables(String hostId,User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        
		List< HostVariable> hvars = hostVariableFactory.getAllVariables();
		Host host = hostAPI.find(hostId, user, respectFrontendRoles);
		if(!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to read the host variables " );
		
		return hvars;
		
	}

	@CloseDBIfOpened
	public List<HostVariable > getVariablesForHost(String hostId,User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
         
		Host host = hostAPI.find(hostId, user, respectFrontendRoles);
		if(!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to read the host variables " );

		List< HostVariable> hvars = hostVariableFactory.getVariablesForHost(hostId);
		
		return hvars;
		
		
	}
	
	@WrapInTransaction
	public void save( HostVariable object, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		Host host = hostAPI.find(object.getHostId(), user, respectFrontendRoles);
		if(!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to edit the host = " + object.getId());
		
		hostVariableFactory.save(object);
		new SiteLoader().invalidate(host);

		
		
	}

	@WrapInTransaction
	public List<HostVariable> save(final List<HostVariable> siteVariables, final String siteId,
			final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException, LanguageException {

		// Validating the user has the proper permissions to edit the host
		Host host = hostAPI.find(siteId, user, respectFrontendRoles);
		if (!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_EDIT, user,
				respectFrontendRoles)) {
			throw new DotSecurityException(
					String.format("User doesn't have permission to edit the host [%s]", siteId)
			);
		}

		// Removing duplicates from the given list of variables
		final var uniqueSiteVariables = getUniqueSiteVariables(siteVariables);

		// Getting all the existing variables for the host
		final List<HostVariable> existingVariables = getVariablesForHost(
				siteId, user, respectFrontendRoles
		);

		// Now we need to loop through the new variables save/update them
		List<HostVariable> processedVariables = new ArrayList<>();
		for (HostVariable siteVariable : uniqueSiteVariables) {

			final HostVariable toProcess;

			// Trying to find a match for the current variable
			Optional<HostVariable> match = searchForMatch(siteVariable, existingVariables);
			if (match.isPresent()) {
				toProcess = new HostVariable(match.get());
				toProcess.setKey(siteVariable.getKey());
				toProcess.setValue(siteVariable.getValue());
				toProcess.setName(siteVariable.getName());

				// Useful for the comparison
				siteVariable.setId(toProcess.getId());
			} else {
				toProcess = new HostVariable(siteVariable);
			}

			// Simple comparison to avoid unnecessary updates
			if (!variableEquals(siteVariable, match.orElse(null))) {
				processedVariables.add(hostVariableFactory.save(toProcess));
			} else {
				processedVariables.add(toProcess);
			}
		}

		// Finally we need to find the variables that were removed
		for (HostVariable existingVariable : existingVariables) {
			Optional<HostVariable> match = searchForMatch(existingVariable, processedVariables);
			if (match.isEmpty()) {
				hostVariableFactory.delete(existingVariable);
			}
		}

		// Invalidate the cache
		new SiteLoader().invalidate(host);

		// Sorting processedVariables by key
		processedVariables.sort(Comparator.comparing(HostVariable::getKey));

		return processedVariables;
	}

	/**
	 * Retrieves a list of unique site variables from the given list of site variables.
	 *
	 * @param siteVariables The list of site variables.
	 * @return A new ArrayList containing only the unique site variables based on their keys.
	 */
	private List<HostVariable> getUniqueSiteVariables(List<HostVariable> siteVariables) {

		return ImmutableList.copyOf(siteVariables.stream().
				collect(Collectors.toMap(
						HostVariable::getKey, // key is the HostVariable's `key`
						hostVariable -> hostVariable,  // value is the HostVariable itself
						(existing, replacement) -> existing)) // if there's a conflict, keep the existing
				.values()
		);
	}

	/**
	 * Checks if two HostVariables are equal based on their key, value, name, and id attributes.
	 *
	 * @param siteVariable      The first HostVariable to compare.
	 * @param otherSiteVariable The second HostVariable to compare.
	 * @return true if the two variables are equal, false otherwise.
	 */
	private boolean variableEquals(HostVariable siteVariable, HostVariable otherSiteVariable) {

		if (otherSiteVariable == null) {
			return false;
		}

		if (siteVariable == otherSiteVariable) {
			return true;
		}

		return siteVariable.getKey().equals(otherSiteVariable.getKey()) &&
				siteVariable.getValue().equals(otherSiteVariable.getValue()) &&
				siteVariable.getName().equals(otherSiteVariable.getName()) &&
				siteVariable.getId().equals(otherSiteVariable.getId());
	}

	/**
	 * Searches for a matching HostVariable in a list of HostVariables.
	 *
	 * @param siteVariable           The HostVariable to search for a match.
	 * @param toCompareSiteVariables The list of HostVariables to compare against.
	 * @return Optional containing the matching HostVariable, or an empty Optional if no match is
	 * found.
	 */
	private Optional<HostVariable> searchForMatch(final HostVariable siteVariable,
			final List<HostVariable> toCompareSiteVariables) {

		// First we need to check by id
		for (final HostVariable next : toCompareSiteVariables) {
			if (next.getId().equals(siteVariable.getId())) {
				return Optional.of(next);
			}
		}

		// No matches found by id, now we need to check by key
		for (final HostVariable next : toCompareSiteVariables) {
			if (next.getKey().equals(siteVariable.getKey())) {
				return Optional.of(next);
			}
		}

		return Optional.empty();
	}

	@Override
    @WrapInTransaction
    public void updateUserReferences(final String userToDelete, final String userToReplace) throws DotDataException {

	    hostVariableFactory.updateUserReferences(userToDelete, userToReplace);

        
    }

	@WrapInTransaction
	public HostVariable copy(HostVariable sourceVariable, Host destinationHost, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		if(!permissionAPI.doesUserHavePermission(destinationHost, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to edit the host = " + destinationHost.getIdentifier());
		HostVariable newVariable = new HostVariable();
		newVariable.setHostId(destinationHost.getIdentifier());
		newVariable.setKey(sourceVariable.getKey());
		newVariable.setLastModDate(new Date());
		newVariable.setLastModifierId(user.getUserId());
		newVariable.setName(sourceVariable.getName());
		newVariable.setValue(sourceVariable.getValue());
		hostVariableFactory.save(newVariable);
		return newVariable;
	}

}