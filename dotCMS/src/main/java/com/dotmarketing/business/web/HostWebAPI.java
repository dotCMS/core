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

	public Host getCurrentHost(final HttpServletRequest request, final PageMode mode) throws DotDataException, DotSecurityException, PortalException, SystemException;

	public Host getHost(HttpServletRequest request);

	public Host getCurrentHostNoThrow(HttpServletRequest request);
	
}
