package com.dotmarketing.business;

import java.util.HashMap;
import java.util.List;

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

public interface UserProxyAPI {

	public UserProxy getUserProxy(String userId, User user, boolean respectFrontEndRoles) throws DotRuntimeException, DotSecurityException, DotDataException;

	public UserProxy getUserProxy(User userToGetProxyFor, User user, boolean respectFrontEndRoles) throws DotRuntimeException, DotSecurityException, DotDataException;

	public void saveUserProxy(UserProxy userProxy, User user, boolean respectFrontEndRoles) throws DotRuntimeException, DotDataException, DotSecurityException;
	

}
