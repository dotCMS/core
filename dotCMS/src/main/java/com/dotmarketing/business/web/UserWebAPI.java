package com.dotmarketing.business.web;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotRuntimeException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

/**
 * Specialized UserAPI created to manage web requests.  
 * @author David
 * @version 1.6
 * @since 1.6
 */
public interface UserWebAPI extends UserAPI {

	/**
	 * 
	 * @param req
	 * @return The logged in user (back-end or front-end), null if no user is logged in 
	 * @throws DotRuntimeException
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	public abstract User getLoggedInUser(HttpServletRequest req) throws DotRuntimeException, PortalException, SystemException;

	/**
	 * 
	 * @param req
	 * @return The logged in user (back-end or front-end), null if no user is logged in 
	 * @throws DotRuntimeException
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	public abstract User getLoggedInFrontendUser(HttpServletRequest req) throws DotRuntimeException, PortalException, SystemException;

	/**
	 * Returns true if the request is coming from a user logged in to the backend
	 * @param req
	 * @return  
	 * @throws DotRuntimeException
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	public abstract boolean isLoggedToBackend(HttpServletRequest req) throws DotRuntimeException, PortalException, SystemException;

	/**
	 * Returns true if the request is coming from a user logged in to the frontend, false otherwise
	 * @param req
	 * @return  
	 * @throws DotRuntimeException
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	public abstract boolean isLoggedToFrontend(HttpServletRequest req) throws DotRuntimeException, PortalException, SystemException;
}