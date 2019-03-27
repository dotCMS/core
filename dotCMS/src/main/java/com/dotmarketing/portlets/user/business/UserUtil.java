package com.dotmarketing.portlets.user.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class UserUtil {
	/**
	 * returns the last host viewed by the user
	 * @param user
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public static Host getLastHost(User user) throws DotDataException, DotSecurityException {

		Host h= APILocator.getHostAPI().find(user.getFavoriteActivity(), user, false);
		if(h != null) 
			return h;
		else
			throw new DotDataException("No mathcing host found");
		
	}

	/**
	 * Sets the last host viewed by the user
	 * @param user
	 * @param host
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public static void setLastHost(User user, Host host) throws DotSecurityException, DotDataException {

		if(user ==null){
			return;
		}
		else if(host ==null){
			user.setFavoriteActivity(null);
			APILocator.getUserAPI().save(user, APILocator.getUserAPI().getSystemUser(), false);
			return;
		}
		else if (APILocator.getPermissionAPI().doesUserHavePermission(host, APILocator.getPermissionAPI().PERMISSION_READ,
				user)) {

			user.setFavoriteActivity(host.getIdentifier());
			APILocator.getUserAPI().save(user, APILocator.getUserAPI().getSystemUser(), false);
			return;
		} else {
			throw new DotSecurityException("User does not have permission to host " + host.getHostname() + " : "
					+ host.getIdentifier());

		}

	}

}
