/**
 * Copyright (c) 2000-2004 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dotmarketing.portlets.report.action;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.PermissionAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.report.factories.ReportFactory;
import com.dotmarketing.portlets.report.factories.ReportParameterFactory;
import com.dotmarketing.portlets.report.model.Report;
import com.dotmarketing.portlets.webforms.factories.WebFormFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.FileUtil;
import com.liferay.util.servlet.SessionMessages;

/**
 *
 * @author  Jason Tesser
 *
 */
public class ViewReportsAction extends DotPortletAction {

	public static final String REPORT_EDITOR_OR_ADMIN = "hasEditorOrAdmin";
	private static PermissionAPI permissionAPI;
	
	static {
		permissionAPI = APILocator.getPermissionAPI();
	}
	
	/*
	 * Render View Report Portlet
	 * @see com.liferay.portal.struts.PortletAction#render(org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm, javax.portlet.PortletConfig, javax.portlet.RenderRequest, javax.portlet.RenderResponse)
	 */
	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
		throws Exception {

        Logger.debug(this, "Running ViewReportsAction!!!!");

		try {
			String[] formTypes = WebFormFactory.getWebFormsTypes();
			ArrayList<PermissionAsset> pas = ReportFactory.getAllReports();
			Report report, newReport;
//			User user = PublicUserFactory.getDefaultUser();
//			List<Role> userRoles;
			Permission permission;
			boolean reportExists;
			for (String formType: formTypes) {
				reportExists = false;
				for (PermissionAsset asset: pas) {
					report = (Report) asset.getAsset();
					if (formType.equals(report.getReportName()) && report.isWebFormReport()) {
						reportExists = true;
						break;
					}
				}
				if (!reportExists) {
					newReport = new Report();
					newReport.setReportName(formType);
					newReport.setReportDescription(formType);
					newReport.setDs("None");
					newReport.setWebFormReport(true);
					ReportFactory.saveReport(newReport);
					/*
					userRoles = com.dotmarketing.business.APILocator.getRoleAPI().addRoleToUser(user.getUserId());
					for (Role role: userRoles) {
						permission = new Permission(newReport.getInode(), Long.parseLong(role.getId()), permissionAPI.PERMISSION_READ);
						permissionAPI.save(permission);
						permission = new Permission(newReport.getInode(), Long.parseLong(role.getId()), permissionAPI.PERMISSION_USE);
						permissionAPI.save(permission);
						permission = new Permission(newReport.getInode(), Long.parseLong(role.getId()), permissionAPI.PERMISSION_WRITE);
						permissionAPI.save(permission);
						permission = new Permission(newReport.getInode(), Long.parseLong(role.getId()), permissionAPI.PERMISSION_EDIT);
						permissionAPI.save(permission);
						permission = new Permission(newReport.getInode(), Long.parseLong(role.getId()), permissionAPI.PERMISSION_PUBLISH);
						permissionAPI.save(permission);
					}
					*/
					
					User systemUser = APILocator.getUserAPI().getSystemUser();
					//Role role = APILocator.getRoleAPI().loadRoleByKey("Report Administrator");
					/*
					permission = new Permission(newReport.getInode(), role.getId(), permissionAPI.PERMISSION_READ);
					permissionAPI.save(permission, newReport, systemUser, false);
					permission = new Permission(newReport.getInode(), role.getId(), permissionAPI.PERMISSION_USE);
					permissionAPI.save(permission, newReport, systemUser, false);
					permission = new Permission(newReport.getInode(), role.getId(), permissionAPI.PERMISSION_WRITE);
					permissionAPI.save(permission, newReport, systemUser, false);
					permission = new Permission(newReport.getInode(), role.getId(), permissionAPI.PERMISSION_EDIT);
					permissionAPI.save(permission, newReport, systemUser, false);
					permission = new Permission(newReport.getInode(), role.getId(), permissionAPI.PERMISSION_PUBLISH);
					permissionAPI.save(permission, newReport, systemUser, false);
					*/
				}
			}
			
			User user = _getUser(req);
			pas = ReportFactory.getAllReports(user);
			req.setAttribute(WebKeys.Report.ReportList,pas);

			List<Role> roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
			req.setAttribute(ViewReportsAction.REPORT_EDITOR_OR_ADMIN, false);
			for (Role role : roles) {
				if(role.getName().equals("Report Administrator") || role.getName().equals("Report Editor") || role.getName().equals("CMS Administrator")){
					req.setAttribute(ViewReportsAction.REPORT_EDITOR_OR_ADMIN, true);
					break;
				}
			}

			if (req.getWindowState().equals(WindowState.NORMAL)) {
				return mapping.findForward("portlet.ext.report.view");
			}
			return mapping.findForward("portlet.ext.report.view_reports");
		}
		catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_ERROR);
		}
	}
	
	/*
	 * Process View Portlet requested actions
	 * @see com.liferay.portal.struts.PortletAction#processAction(org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm, javax.portlet.PortletConfig, javax.portlet.ActionRequest, javax.portlet.ActionResponse)
	 */
	@Override
	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res)throws DotHibernateException  {
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		String cmd = req.getParameter(Constants.CMD);
        Logger.debug(this, "Inside ViewReportAction cmd=" + cmd);


        if ((cmd != null) && cmd.equals(Constants.DELETE)) {
				ArrayList<String> adminRoles = new ArrayList<String>();
				adminRoles.add("CMS Administrator");
				String reportToDeleteId = req.getParameter("reportToDelete");
				String[] reportIds = null;
				if (UtilMethods.isSet(req.getParameter("reportsToDelete"))) {
					if (req.getParameter("reportsToDelete") instanceof String)
						reportIds = req.getParameter("reportsToDelete").split(",");
					else
						reportIds = req.getParameterValues("reportsToDelete");
				}
	        	if(reportIds != null){
		        	for (String reportId : reportIds) {
		        		try{
		        			Report report = ReportFactory.getReport(reportId);
		        			_checkDeletePermissions(report, _getUser(req), httpReq, adminRoles);
		        			deleteReport(report);
		        		}catch(Exception ex){
		        			HibernateUtil.rollbackTransaction();
		        			SessionMessages.add(req,"error", "message.report.delete.error");
		        			Logger.error(this, "Problem Deleting Report with inode " + reportId, ex);
		        		}
					}
	        	}else if(reportToDeleteId != null){
	        		try{
	        			Report report = ReportFactory.getReport(reportToDeleteId);
	        			_checkDeletePermissions(report, _getUser(req), httpReq, adminRoles);
	        			deleteReport(report);
	        		}catch(Exception ex){
	        			HibernateUtil.rollbackTransaction();
	        			SessionMessages.add(req,"error", "message.report.delete.error");
	        			Logger.error(this, "Problem Deleting Report with inode " + reportToDeleteId, ex);
	        		}
	        	}else{
	        		SessionMessages.add(req,"error", "message.report.delete.error");
	        	}
//        	}
        }
	}
	
	/*
	 * Delete a seleted report
	 * @param		report Report to delete
	 * @exception	Exception
	 */
	private void deleteReport(Report report) throws Exception{
        File f;
        String jrxmlPath;
        String jasperPath;
		HibernateUtil.startTransaction();
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		perAPI.removePermissions(report);
	    ReportFactory.deleteReport(report);	    
	    ReportParameterFactory.deleteReportsParameters(report);	        	
	    WebFormFactory.removeWebFormsByType(report.getReportName());
	    if (UtilMethods.isSet(Config.getStringProperty("ASSET_REAL_PATH"))) {
	    	jrxmlPath = Config.getStringProperty("ASSET_REAL_PATH") + File.separator + Config.getStringProperty("REPORT_PATH") + File.separator + report.getInode() + ".jrxml";
		    jasperPath = Config.getStringProperty("ASSET_REAL_PATH") + File.separator + Config.getStringProperty("REPORT_PATH") + File.separator + report.getInode() + ".jasper";
	    } else {
	    	jrxmlPath = FileUtil.getRealPath(File.separator + Config.getStringProperty("ASSET_PATH") + File.separator + Config.getStringProperty("REPORT_PATH") + File.separator + report.getInode() + ".jrxml");
	    	jasperPath = FileUtil.getRealPath(File.separator + Config.getStringProperty("ASSET_PATH") + File.separator + Config.getStringProperty("REPORT_PATH") + File.separator + report.getInode() + ".jasper");
	    }
		f = new File(jrxmlPath);
		f.delete();
		f = new File(jasperPath);
		f.delete();
		//Delete the data in the DB if is a webFormReport
		if(report.isWebFormReport())
		{
			String formType = report.getReportName();			
			WebFormFactory.removeWebFormsByType(formType);
		}
	    HibernateUtil.commitTransaction();
	}
}