package com.dotmarketing.portlets.contentlet.util;

import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class HostUtils {
	
	private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	    
	
	
	/**
	 * Filters the given host with the given permission, if the user does not have the given permission on the host it looks for all hosts 
	 * and returns the id of the first host in the list for which the user has the given permission, otherwise returns the given id.
	 * @param selectedHostId
	 * @param permissionInt
	 * @param user
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public static String filterDefaultHostForSelect(String selectedHostId, int permissionInt, User user) throws DotDataException, DotSecurityException{
		if(UtilMethods.isSet(selectedHostId)){
			Host host = APILocator.getHostAPI().find(selectedHostId, user, false);
			if(host!=null){
				if(permissionAPI.doesUserHavePermission(host, permissionInt, user)){
					return selectedHostId;
				}
			}
			List<Host> hosts = APILocator.getHostAPI().findAll(user, false);
			hosts = permissionAPI.filterCollection(hosts, permissionInt, false, user);
			if(!hosts.isEmpty()){
				return hosts.get(0).getIdentifier();
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param selectedHostId
	 * @param requiredPermissions
	 * @param user
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public static String filterDefaultHostForSelect(String selectedHostId, String requiredPermissions, User user) throws DotDataException, DotSecurityException{
		if(UtilMethods.isSet(selectedHostId)){
			Host host = APILocator.getHostAPI().find(selectedHostId, user, false);
			if(host!=null){
				if(permissionAPI.doesUserHavePermissions(host, requiredPermissions, user)){
					return selectedHostId;
				}
			}
			List<Host> hosts = APILocator.getHostAPI().findAll(user, false);
			for(Host h: hosts){
				if(permissionAPI.doesUserHavePermissions(h, requiredPermissions, user)){
					return h.getIdentifier();
				}
			}

		}
		return null;
	}



}
