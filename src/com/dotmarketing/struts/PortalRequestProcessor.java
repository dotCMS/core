/*
 * Created on Feb 17, 2005
 *
 */
package com.dotmarketing.struts;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionMapping;

import com.dotmarketing.util.Config;


/**
 * @author will
 * 
 */
public class PortalRequestProcessor extends
		com.liferay.portal.struts.PortalRequestProcessor {

	protected String processPath(HttpServletRequest req, HttpServletResponse res)
			throws IOException {
		String path = null;
		if (req.getRequestURI().startsWith(Config.getStringProperty("CMS_STRUTS_PATH"))) {
			path = super.callParentProcessPath(req, res);
		} else {
			path = super.processPath(req, res);
		}
		return path;
	}

	protected boolean processRoles(
			HttpServletRequest req, HttpServletResponse res,
			ActionMapping mapping)
		throws IOException, ServletException {

		if (req.getRequestURI().startsWith(Config.getStringProperty("CMS_STRUTS_PATH"))) {
			return super.callParentProcessRoles(req,res,mapping); 
		} else {
			return super.processRoles(req,res,mapping);
		}
	
	}
	
	protected void doForward(
			String uri, HttpServletRequest req, HttpServletResponse res)
		throws IOException, ServletException {
		if (req.getRequestURI().startsWith(Config.getStringProperty("CMS_STRUTS_PATH"))) {
			req.getRequestDispatcher(uri).forward(req,res);
		} else {
			super.doForward(uri, req, res);
		}

	}
}