package com.dotmarketing.portlets.report.struts;

import java.util.ArrayList;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.util.Constants;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

/** @author Jason Tesser */
public class ReportForm extends ValidatorForm {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String reportId;
	public String reportName;
	public String reportDescription;
	public ArrayList<DataSource> dataSources;
	public String selectedDataSource;
	private boolean webFormReport;
	
	private String owner;
	
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
	 * @return the reportId
	 */
	public String getReportId() {
		return reportId;
	}
	/**
	 * @param reportId the reportId to set
	 */
	public void setReportId(String reportId) {
		this.reportId = reportId;
	}
	/**
	 * @return the dataSources
	 */
	public ArrayList<DataSource> getDataSources() {
		return dataSources;
	}
	/**
	 * @param dataSources the dataSources to set
	 */
	public void setDataSources(ArrayList<DataSource> dataSources) {
		this.dataSources = dataSources;
	}
	/**
	 * @return the selectedDataSource
	 */
	public String getSelectedDataSource() {
		return selectedDataSource;
	}
	/**
	 * @param selectedDataSource the selectedDataSource to set
	 */
	public void setSelectedDataSource(String selectedDataSource) {
		if(selectedDataSource.equals("DotCMS Datasource")){
			this.selectedDataSource = Constants.DATABASE_DEFAULT_DATASOURCE;
		}else{
			this.selectedDataSource = selectedDataSource;
		}
	}
	
	public class DataSource{
		String dsName;

		public DataSource() {
			// TODO Auto-generated constructor stub
		}
		public DataSource(String dsName) {
			this.dsName = dsName;
		}
		
		/**
		 * @return the dsName
		 */
		public String getDsName() {
			return dsName;
		}

		/**
		 * @param dsName the dsName to set
		 */
		public void setDsName(String dsName) {
			if(dsName.equals("DotCMS Datasource")){
				this.dsName = Constants.DATABASE_DEFAULT_DATASOURCE;
			}else{
				this.dsName = dsName;
			}
		}
	}
	
	/**
	 * @return the dataSource
	 */
	public DataSource getNewDataSource(){
		return new DataSource();
	}
	
	/**
	 * @return the webFormReport
	 */
	public boolean isWebFormReport() {
		return webFormReport;
	}
	
	
	
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	/**
	 * @param webFormReport the webFormReport to set
	 */
	public void setWebFormReport(boolean webFormReport) {
		this.webFormReport = webFormReport;
	}
	
	/*
	 * Validator for the Report Form
	 * @see org.apache.struts.validator.ValidatorForm#validate(org.apache.struts.action.ActionMapping, javax.servlet.http.HttpServletRequest)
	 */
	public ActionErrors validate(org.apache.struts.action.ActionMapping mapping, javax.servlet.http.HttpServletRequest request,User user)throws Exception {
		ActionErrors errors = new ActionErrors();
		
		if (!UtilMethods.isSet(reportName)) {
			errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required",LanguageUtil.get(user, "Name")));    		
		}
		
		if (!UtilMethods.isSet(reportDescription)) {
			errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required",LanguageUtil.get(user, "Description")));    		
		}
		
		if (!isWebFormReport()) {
			if (!UtilMethods.isSet(selectedDataSource)) {
				errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required",LanguageUtil.get(user, "Datasource")));    		
			}
			
			if (!UtilMethods.isSet(request.getParameter("jrxmlFile"))) {
				errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required",LanguageUtil.get(user, "JRXML-File")));    		
			}
		}
		
		return errors;
	}
	
	
}