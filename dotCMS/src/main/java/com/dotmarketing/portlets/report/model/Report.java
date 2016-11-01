package com.dotmarketing.portlets.report.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;

/**
*
* @author  Jason Tesser
*
*/

public class Report extends Inode implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reportName;
    private String reportDescription;
    private boolean requiresInput;
    private String ds;
    private boolean webFormReport;
	private ArrayList<ReportParameter> parameters;
    
	public static long getSerialVersionUID() {
		return serialVersionUID;
	}
	
	public Report() {
		super.setType("report_asset");
	}
	
	/**
	 * @return the reportDescription
	 */
	public String getReportDescription() {
		return reportDescription;
	}
	/**
	 * @param reportDescription the reportDescription to set
	 */
	public void setReportDescription(String reportDescription) {
		this.reportDescription = reportDescription;
	}
	/**
	 * @return the reportName
	 */
	public String getReportName() {
		return reportName;
	}
	/**
	 * @param reportName the reportName to set
	 */
	public void setReportName(String reportName) {
		this.reportName = reportName;
	}

	/**
	 * @return the requiresInput
	 */
	public boolean isRequiresInput() {
		return requiresInput;
	}

	/**
	 * @param requiresInput the requiresInput to set
	 */
	public void setRequiresInput(boolean requiresInput) {
		this.requiresInput = requiresInput;
	}

	/**
	 * @return the ds
	 */
	public String getDs() {
		return ds;
	}

	/**
	 * @param ds the ds to set
	 */
	public void setDs(String ds) {
		this.ds = ds;
	}
	
	/**
	 * @return the webFormReport
	 */
	public boolean isWebFormReport() {
		return webFormReport;
	}
	
	/**
	 * @param webFormReport the webFormReport to set
	 */
	public void setWebFormReport(boolean webFormReport) {
		this.webFormReport = webFormReport;
	}

	/**
	 * @return the parameters
	 */
	public ArrayList<ReportParameter> getParameters() {
		return parameters;
	}

	/**
	 * @param parameters the parameters to set
	 */
	public void setParameters(ArrayList<ReportParameter> parameters) {
		this.parameters = parameters;
	}
	
	/**
	 * List of permissions it accepts
	 */
	public List<PermissionSummary> acceptedPermissions() {
		List<PermissionSummary> accepted = new ArrayList<PermissionSummary>();
		accepted.add(new PermissionSummary("view", "view-permission-description", PermissionAPI.PERMISSION_READ));
		accepted.add(new PermissionSummary("edit", "edit-permission-description", PermissionAPI.PERMISSION_WRITE));
		accepted.add(new PermissionSummary("edit-permissions", "edit-permissions-permission-description", PermissionAPI.PERMISSION_EDIT_PERMISSIONS));
		return accepted;
	}
	
}