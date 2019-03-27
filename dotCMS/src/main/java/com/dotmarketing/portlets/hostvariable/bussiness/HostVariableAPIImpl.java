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

import java.util.Date;
import java.util.List;

import com.liferay.portal.model.User;

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