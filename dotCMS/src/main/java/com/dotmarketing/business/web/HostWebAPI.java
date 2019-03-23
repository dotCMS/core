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
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

import javax.servlet.http.HttpServletRequest;

/**
 * 
 * @author david torres
 *
 */
public interface HostWebAPI extends HostAPI {

	public Host getCurrentHost(RenderRequest req) throws DotDataException, DotSecurityException, PortalException, SystemException;
	
	public Host getCurrentHost(ActionRequest req) throws DotDataException, DotSecurityException, PortalException, SystemException;
	
	public Host getCurrentHost(HttpServletRequest req) throws DotDataException, DotSecurityException, PortalException, SystemException;

	public Host getHost(HttpServletRequest request);

	/**
	 * This method will just look for the host on the current request or session (if exists) null if not any
	 * Won't implement any fallback or logic to find an object, see {@link #getCurrentHost(HttpServletRequest)}
	 * @param request {@link HttpServletRequest}
	 * @return Host
	 */
	public Host findHostOnRequest (final HttpServletRequest request);

	public Host getCurrentHostNoThrow(HttpServletRequest request);
	
}
