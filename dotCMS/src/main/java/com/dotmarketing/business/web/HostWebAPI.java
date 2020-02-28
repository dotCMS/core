/**
 * 
 */
package com.dotmarketing.business.web;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Optional;

/**
 * 
 * @author david torres
 *
 */
public interface HostWebAPI extends HostAPI {

	public Host getCurrentHost(RenderRequest req) throws DotDataException, DotSecurityException, PortalException, SystemException;
	
	public Host getCurrentHost(ActionRequest req) throws DotDataException, DotSecurityException, PortalException, SystemException;
	
	public Host getCurrentHost(HttpServletRequest req) throws DotDataException, DotSecurityException, PortalException, SystemException;

	/**
	 * Return the current host using the current request
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Host getCurrentHost() throws DotDataException, DotSecurityException;

	/**
	 * Return the current Host taking the host id or name from any of the follow source:
	 *
	 * - Current host id set by 'host_id' {@link HttpServletRequest} parameter, just in case the log in user is a backend user.
	 * - Current host name set by 'Host' {@link HttpServletRequest} parameter
	 * - Current {@link Host} object set by {@link WebKeys#CURRENT_HOST} {@link HttpServletRequest} attribute.
	 * - Current {@link Host} object set by {@link WebKeys#CURRENT_HOST} {@link HttpSession} attribute,
	 * - Current host id set by {@link WebKeys#CMS_SELECTED_HOST_ID} {@link HttpSession} attribute,
	 * 	 just in case the log in user is a backend user..
	 *   just in case the log in user is a backend user..
	 * - Current host name get by {@link HttpServletRequest#getServerName()}
	 * - Default Host
	 *
	 * @param request current {@link HttpServletRequest}
	 * @param user User current login user
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws PortalException
	 * @throws SystemException
	 */
	public Host getCurrentHost(final HttpServletRequest request, final User user) throws DotDataException, DotSecurityException;

	public Host getHost(HttpServletRequest request);

	public Host getCurrentHostNoThrow(HttpServletRequest request);
	
}
