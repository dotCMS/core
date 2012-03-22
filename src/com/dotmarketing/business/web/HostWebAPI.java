/**
 * 
 */
package com.dotmarketing.business.web;

import javax.portlet.ActionRequest;
import javax.portlet.RenderRequest;
import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

/**
 * 
 * @author david torres
 *
 */
public interface HostWebAPI extends HostAPI {

	public Host getCurrentHost(RenderRequest req) throws DotDataException, DotSecurityException, PortalException, SystemException;
	
	public Host getCurrentHost(ActionRequest req) throws DotDataException, DotSecurityException, PortalException, SystemException;
	
	public Host getCurrentHost(HttpServletRequest req) throws DotDataException, DotSecurityException, PortalException, SystemException;
	
}
