package com.dotmarketing.portlets.cmsmaintenance.struts;

import java.util.List;

import org.apache.struts.action.ActionForm;

import com.dotmarketing.portlets.structure.model.Structure;
/**
 *  This class form is created to manipulate the maintenance task
 *  of the CMS Maintenance portlet
 *  
 * @author Oswaldo
 *
 */
public class CmsMaintenanceForm extends ActionForm {
	
	private static final long serialVersionUID = 1L;
	private String cacheName = "";  
	private String searchString="";
	private String replaceString="";
	private String userId="";
	private List<Structure> structures;
	private String structure;
	private String removeassetsdate;
	
	public String getRemoveassetsdate() {
		return removeassetsdate;
	}
	public void setRemoveassetsdate(String removeassetsdate) {
		this.removeassetsdate = removeassetsdate;
	}
	public String getStructure() {
		return structure;
	}
	public void setStructure(String structure) {
		this.structure = structure;
	}
	public List<Structure> getStructures() {
		return structures;
	}
	public void setStructures(List<Structure> structures) {
		this.structures = structures;
	}
	/**
	 * @return Returns the cacheName.
	 */
	public String getCacheName() 
	{
		return cacheName;
	}
	/**
	 * @param cacheName The cacheName to set.
	 */
	public void setCacheName(String cacheName) 
	{
		this.cacheName = cacheName;
	}
	
	/**
	 * @return Returns the replaceString.
	 */
	public String getReplaceString() {
		return replaceString;
	}
	
	/**
	 * @param replaceString The replaceString to set.
	 */
	public void setReplaceString(String replaceString) {
		this.replaceString = replaceString;
	}
	
	/**
	 * @return Returns the searchString.
	 */
	public String getSearchString() {
		return searchString;
	}
	
	/**
	 * @param searchString The searchString to set.
	 */
	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}
	
	/**
	 * @return Returns the userId.
	 */
	public String getUserId() {
		return userId;
	}
	
	/**
	 * @param userId The userId to set.
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

}

