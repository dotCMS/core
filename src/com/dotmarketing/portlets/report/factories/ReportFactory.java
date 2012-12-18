/**
 *  Package com.dotmarketing.portlets.report.factories
 *
 * @author Armando Siem
 */

package com.dotmarketing.portlets.report.factories;

import java.util.ArrayList;
import java.util.HashMap;

import com.dotmarketing.beans.PermissionAsset;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.report.model.Report;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Parameter;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Factory class for the dotcms report entity
 *
 * @author  Armando Siem
 */

public class ReportFactory {
	
	/**
	  * Get all the report visible or editable by a user
	  * @param		user User
	  * @return		ArrayList<PermissionAsset>
	 * @throws DotDataException 
	  */
	public static ArrayList<PermissionAsset> getAllReports(User user) throws DotDataException{
		ArrayList<PermissionAsset> permissionReports = new ArrayList<PermissionAsset>();
		String roles = "";
		ArrayList<Role> rls = new ArrayList<Role>(com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId()));
		boolean isAdmin = false;
		isAdmin = true;
		for (Role role : rls) {
			if(roles != ""){
				roles += ",";
			}
			roles += "'" + role.getId() + "'";
		}
		StringBuffer sb = new StringBuffer();
		
		sb.append("select r.*, i.owner ,max(p.permission) as max_permission ");
		sb.append("from report_asset r ");
		sb.append("inner join inode i on (r.inode = i.inode)");
		sb.append("left join permission p on (r.inode = p.inode_id and p.roleid in (" + roles + ")) ");
		sb.append("group by r.inode,r.report_name,r.report_description,r.requires_input,r.ds,r.web_form_report,i.owner ");
		sb.append("order by r.report_name ");
		
		DotConnect dc = new DotConnect();
		dc.setSQL(sb.toString());
		ArrayList<HashMap<String, String>> results = dc.getResults();
		for (HashMap<String, String> map : results) {
			PermissionAsset pa = new PermissionAsset();
			Report report = new Report();
			report.setInode(map.get("inode"));
			report.setReportName(map.get("report_name"));
			report.setReportDescription(map.get("report_description"));
			report.setRequiresInput(Parameter.getBooleanFromString(map.get("requires_input")));
			report.setOwner(map.get("owner"));
			report.setWebFormReport(Parameter.getBooleanFromString(map.get("web_form_report")));
			ArrayList p = new ArrayList();
			if(isAdmin || user.equals(map.get("owner"))){
				p.add(new Long(4));
			}else{
				p.add(new Long(UtilMethods.parseLong(map.get("max_permission"),0)));
			}
			pa.setPermissions(p);
			pa.setAsset(report);
			permissionReports.add(pa);
		}
		return permissionReports;
	}
	
	/**
	  * Get a specific report
	  * @param		reportId
	  * @return		Report
	  */
	public static Report getReport(String reportId)throws DotHibernateException{
		HibernateUtil hu = new HibernateUtil(Report.class);
		return (Report)hu.load(reportId);
	}
	
	/**
	  * Delete a specific report
	  * @param		reportId
	  */
	public static void deleteReport(Report report)throws DotHibernateException{
		HibernateUtil.delete(report);
	}
	
	/**
	  * Save a specific report
	  * @param		reportId
	  */
	public static void saveReport(Report report)throws DotHibernateException{
		HibernateUtil.save(report);
	}
	
	/**
	  * Get all the report
	  * @return		ArrayList<PermissionAsset>
	  */
	public static ArrayList<PermissionAsset> getAllReports(){
		ArrayList<PermissionAsset> permissionReports = new ArrayList<PermissionAsset>();
		StringBuffer sb = new StringBuffer();
		
		sb.append("select r.*, i.owner ");
		sb.append("from report_asset r ");
		sb.append("inner join inode i on (r.inode = i.inode) ");
		sb.append("group by r.inode,r.report_name,r.report_description,r.requires_input,r.ds,r.web_form_report,i.owner ");
		sb.append("order by r.report_name ");
		
		DotConnect dc = new DotConnect();
		dc.setSQL(sb.toString());
		ArrayList<HashMap<String, String>> results =null;
		try {
			results = dc.getResults();
		} catch (DotDataException e) {
			Logger.error(ReportFactory.class, e.getMessage(), e);
		}
		for (HashMap<String, String> map : results) {
			PermissionAsset pa = new PermissionAsset();
			Report report = new Report();
			report.setInode(map.get("inode"));
			report.setReportName(map.get("report_name"));
			report.setReportDescription(map.get("report_description"));
			report.setRequiresInput(Parameter.getBooleanFromString(map.get("requires_input")));
			report.setOwner(map.get("owner"));
			report.setWebFormReport(Parameter.getBooleanFromString(map.get("web_form_report")));
			pa.setAsset(report);
			permissionReports.add(pa);
		}
		return permissionReports;
	}
	
	/**
	 * Get all the report with the option of filtering by report type and/or report name
	 * @param      type String with the values "" to get all the report types, "true" to get the Web-Form-Reports or "false" to get the Jasper-Reports
	 * @param      reportname String with the name of the report to search
	 * @return
	 * @throws DotDataException
	 */
	public static ArrayList<PermissionAsset> getAllReports(String type, String reportname) throws DotDataException{
		ArrayList<PermissionAsset> permissionReports = new ArrayList<PermissionAsset>();
		StringBuffer sb = new StringBuffer();
		String condition="";
		
		sb.append("select r.*, i.owner ");
		sb.append("from report_asset r ");
		sb.append("inner join inode i on (r.inode = i.inode) ");
		if(UtilMethods.isSet(reportname)){
			condition=" where lower(r.report_name) like '%"+reportname.toLowerCase()+"%' ";
		}
		if(UtilMethods.isSet(type)){
			boolean isWebFormReport = DbConnectionFactory.isDBTrue(type);
			if(UtilMethods.isSet(condition)){
				condition=condition+" and r.web_form_report ="+isWebFormReport+" ";
			}else{
				condition=" where r.web_form_report ="+isWebFormReport+" ";
			}
		}
		if(UtilMethods.isSet(condition)){
			sb.append(condition);
		}
		sb.append("group by r.inode,r.report_name,r.report_description,r.requires_input,r.ds,r.web_form_report,i.owner ");
		sb.append("order by r.report_name ");
		
		DotConnect dc = new DotConnect();
		dc.setSQL(sb.toString());
		ArrayList<HashMap<String, String>> results =null;
		try {
			results = dc.getResults();
		} catch (DotDataException e) {
			Logger.error(ReportFactory.class, e.getMessage(), e);
		}
		for (HashMap<String, String> map : results) {
			PermissionAsset pa = new PermissionAsset();
			Report report = new Report();
			report.setInode(map.get("inode"));
			report.setReportName(map.get("report_name"));
			report.setReportDescription(map.get("report_description"));
			report.setRequiresInput(Parameter.getBooleanFromString(map.get("requires_input")));
			report.setOwner(map.get("owner"));
			report.setWebFormReport(Parameter.getBooleanFromString(map.get("web_form_report")));
			pa.setAsset(report);
			permissionReports.add(pa);
		}
		return permissionReports;
	}
	
	/**
	  * Get all the report visible or editable by a user with the option of filtering by report type and/or report name too.
	  * @param		user User
	  * @param      type String with the values "" to get all the report types, "true" to get the Web-Form-Reports or "false" to get the Jasper-Reports
	  * @param      reportname String with the name of the report to search
	  * @return		ArrayList<PermissionAsset>
	 * @throws DotDataException 
	  */
	public static ArrayList<PermissionAsset> getAllReports(User user,String type, String reportname) throws DotDataException{
		ArrayList<PermissionAsset> permissionReports = new ArrayList<PermissionAsset>();
		String condition="";
		String roles = "";
		ArrayList<Role> rls = new ArrayList<Role>(com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId()));
		boolean isAdmin = false;
		isAdmin = true;
		for (Role role : rls) {
			if(roles != ""){
				roles += ",";
			}
			roles += "'" + role.getId() + "'";
		}
		StringBuffer sb = new StringBuffer();
		
		sb.append("select r.*, i.owner ,max(p.permission) as max_permission ");
		sb.append("from report_asset r ");
		sb.append("inner join inode i on (r.inode = i.inode)");
		sb.append("left join permission p on (r.inode = p.inode_id and p.roleid in (" + roles + ")) ");
		if(UtilMethods.isSet(reportname)){
			condition=" where lower(r.report_name) like '%"+reportname.toLowerCase()+"%' ";
		}
		if(UtilMethods.isSet(type)){
			boolean isWebFormReport = DbConnectionFactory.isDBTrue(type);
			if(UtilMethods.isSet(condition)){
				condition=condition+" and r.web_form_report ="+isWebFormReport+" ";
			}else{
				condition=" where r.web_form_report ="+isWebFormReport+" ";
			}
		}
		if(UtilMethods.isSet(condition)){
			sb.append(condition);
		}
		sb.append("group by r.inode,r.report_name,r.report_description,r.requires_input,r.ds,r.web_form_report,i.owner ");
		sb.append("order by r.report_name ");
		
		DotConnect dc = new DotConnect();
		dc.setSQL(sb.toString());
		ArrayList<HashMap<String, String>> results = dc.getResults();
		for (HashMap<String, String> map : results) {
			PermissionAsset pa = new PermissionAsset();
			Report report = new Report();
			report.setInode(map.get("inode"));
			report.setReportName(map.get("report_name"));
			report.setReportDescription(map.get("report_description"));
			report.setRequiresInput(Parameter.getBooleanFromString(map.get("requires_input")));
			report.setOwner(map.get("owner"));
			report.setWebFormReport(Parameter.getBooleanFromString(map.get("web_form_report")));
			ArrayList p = new ArrayList();
			if(isAdmin || user.equals(map.get("owner"))){
				p.add(new Long(4));
			}else{
				p.add(new Long(UtilMethods.parseLong(map.get("max_permission"),0)));
			}
			pa.setPermissions(p);
			pa.setAsset(report);
			permissionReports.add(pa);
		}
		return permissionReports;
	}
}
