package com.dotmarketing.portlets.hostvariable.bussiness;

import java.util.Date;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import com.dotmarketing.services.HostServices;
import com.liferay.portal.model.User;

public class HostVariableAPIImpl implements HostVariableAPI {

	HostVariableFactory hvarFactory;
	PermissionAPI perAPI;
	HostAPI hostAPI = APILocator.getHostAPI();
	
	public HostVariableAPIImpl() {
		hvarFactory = FactoryLocator.getHostVariableFactory();
		perAPI = APILocator.getPermissionAPI();
	}
	
	
	public void delete(HostVariable object, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		Host host = hostAPI.find(object.getHostId(), user, respectFrontendRoles);
		if(!perAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to edit the host variable= " + object.getId());
		
		hvarFactory.delete(object);
		HostServices.invalidate(host);
		
	}
	
	public HostVariable find(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		
		HostVariable hvar = hvarFactory.find(id);
		Host host = hostAPI.find(hvar.getHostId(), user, respectFrontendRoles);
		if(!perAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_USE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to read the host variable = " + hvar.getId());
		return hvar;
		
	}
	

	public List<HostVariable > getAllVariables(String hostId,User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        
		List< HostVariable> hvars = hvarFactory.getAllVariables();
		Host host = hostAPI.find(hostId, user, respectFrontendRoles);
		if(!perAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to read the host variables " );
		
		return hvars;
		
	}
	
	public List<HostVariable > getVariablesForHost(String hostId,User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
         
		Host host = hostAPI.find(hostId, user, respectFrontendRoles);
		if(!perAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to read the host variables " );

		List< HostVariable> hvars = hvarFactory.getVariablesForHost(hostId);
		
		return hvars;
		
		
	}
	

	public void save( HostVariable object, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		Host host = hostAPI.find(object.getHostId(), user, respectFrontendRoles);
		if(!perAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to edit the host = " + object.getId());
		
		hvarFactory.save(object);
		HostServices.invalidate(host);
		
	}

	public HostVariable copy(HostVariable sourceVariable, Host destinationHost, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		if(!perAPI.doesUserHavePermission(destinationHost, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to edit the host = " + destinationHost.getIdentifier());
		HostVariable newVariable = new HostVariable();
		newVariable.setHostId(destinationHost.getIdentifier());
		newVariable.setKey(sourceVariable.getKey());
		newVariable.setLastModDate(new Date());
		newVariable.setLastModifierId(user.getUserId());
		newVariable.setName(sourceVariable.getName());
		newVariable.setValue(sourceVariable.getValue());
		hvarFactory.save(newVariable)
;		return newVariable;
	}

}