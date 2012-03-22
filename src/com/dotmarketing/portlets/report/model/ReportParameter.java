package com.dotmarketing.portlets.report.model;

import java.io.Serializable;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.util.InodeUtils;

public class ReportParameter extends Inode implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8971755220627017713L;
	private String inode;
	private String reportInode;
	private String name;
	private String description;
	private String classType;
	private String defaultValue;
	private String value;
	
	public ReportParameter() {
		super.setType("report_parameter");
	}
	
	/**
	 * @return the classType
	 */
	public String getClassType() {
		return classType;
	}
	/**
	 * @param classType the classType to set
	 */
	public void setClassType(String classType) {
		this.classType = classType;
	}
	/**
	 * @return the defaultValue
	 */
	public String getDefaultValue() {
		return defaultValue;
	}
	/**
	 * @param defaultValue the defaultValue to set
	 */
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @return the inode
	 */
	public String getInode() {
		if(InodeUtils.isSet(inode))
			return inode;
		
		return "";
	}
	/**
	 * @param inode the inode to set
	 */
	public void setInode(String inode) {
		this.inode = inode;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the reportInode
	 */
	public String getReportInode() {
		return reportInode;
	}
	/**
	 * @param reportInode the reportInode to set
	 */
	public void setReportInode(String reportInode) {
		this.reportInode = reportInode;
	}
	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}
	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}
}
