package com.dotmarketing.portlets.hostvariable.bussiness;

import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import com.liferay.portal.model.User;

public interface HostVariableAPI {
	
	void delete(HostVariable object, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException ;

	HostVariable find(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException ;
	
	List<HostVariable > getAllVariables(String hostId,User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	List<HostVariable > getVariablesForHost(String hostId,User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException; 
	
	void save( HostVariable object, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	HostVariable copy (HostVariable sourceVariable, Host destinationHost, User user, boolean respectFrontendRoles) 
		throws DotDataException, DotSecurityException;

}
