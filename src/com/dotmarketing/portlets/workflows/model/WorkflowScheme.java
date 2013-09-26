package com.dotmarketing.portlets.workflows.model;

import java.io.Serializable;
import java.util.Date;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.dotmarketing.util.UtilMethods;

public class WorkflowScheme implements Serializable {

	private static final long serialVersionUID = 1L;

	String id;
	Date creationDate = new Date();
	String name;
	String description;
	boolean archived;
	boolean mandatory;
	boolean defaultScheme;
	private Date modDate;
	
	public boolean isDefaultScheme() {
		return defaultScheme;
	}

	public void setDefaultScheme(boolean defaultScheme) {
		this.defaultScheme = defaultScheme;
	}
	String entryActionId;
	
	public String getEntryActionId() {
		return entryActionId;
	}

	public void setEntryActionId(String entryActionId) {
		this.entryActionId = entryActionId;
	}

	@Override
	public String toString() {
		return "WorkflowScheme [id=" + id + ", name=" + name + ", archived=" + archived + ", mandatory=" + mandatory + "]";
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}



	public boolean isArchived() {
		return archived;
	}

	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}
	@JsonIgnore
	public boolean isNew(){
		return !UtilMethods.isSet(id);
		
	}

	public Date getModDate() {
		return modDate;
	}

	public void setModDate(Date modDate) {
		this.modDate = modDate;
	}
	
}
