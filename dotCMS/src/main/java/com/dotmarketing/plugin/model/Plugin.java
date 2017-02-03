package com.dotmarketing.plugin.model;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Jason Tesser
 * @since 1.6.5b
 * A simple POJO for dotCMS plugins
 */

public class Plugin implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7973138268727039183L;
	private String id = "";
	/**
	 * The plugin name is a friendly name for the plugin.  ie.. ACME Help Desk
	 */
	private String pluginName;
	private String pluginVersion;
	private String author;
	private Date firstDeployedDate;
	private Date lastDeployedDate;
	
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	/**
	 * @return the pluginName
	 */
	public String getPluginName() {
		return pluginName;
	}
	/**
	 * @param pluginName the pluginName to set
	 */
	public void setPluginName(String pluginName) {
		this.pluginName = pluginName;
	}
	/**
	 * @return the pluginVersion
	 */
	public String getPluginVersion() {
		return pluginVersion;
	}
	/**
	 * @param pluginVersion the pluginVersion to set
	 */
	public void setPluginVersion(String pluginVersion) {
		this.pluginVersion = pluginVersion;
	}
	/**
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}
	/**
	 * @param author the author to set
	 */
	public void setAuthor(String author) {
		this.author = author;
	}
	/**
	 * @return the firstDeployedDate
	 */
	public Date getFirstDeployedDate() {
		return firstDeployedDate;
	}
	/**
	 * @param firstDeployedDate the firstDeployedDate to set
	 */
	public void setFirstDeployedDate(Date firstDeployedDate) {
		this.firstDeployedDate = firstDeployedDate;
	}
	/**
	 * @return the lastDeployedDate
	 */
	public Date getLastDeployedDate() {
		return lastDeployedDate;
	}
	/**
	 * @param lastDeployedDate the lastDeployedDate to set
	 */
	public void setLastDeployedDate(Date lastDeployedDate) {
		this.lastDeployedDate = lastDeployedDate;
	}
	
}
