package com.dotmarketing.portlets.workflows.model;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.workflows.business.WorkFlowFactory;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vavr.control.Try;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowScheme implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String SYSTEM_WORKFLOW_ID = WorkFlowFactory.SYSTEM_WORKFLOW_ID;

	String id;
	Date creationDate = new Date();
	String name;
	String description;
	boolean archived;
	boolean mandatory;
	boolean defaultScheme;
	private Date modDate = new Date();
	

	public boolean isDefaultScheme() {
		return defaultScheme;
	}

	public void setDefaultScheme(boolean defaultScheme) {
		this.defaultScheme = defaultScheme;
	}
	String entryActionId;

	@Deprecated
	public String getEntryActionId() {
		return entryActionId;
	}

	@Deprecated
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

	@Deprecated
	public boolean isMandatory() {
		return mandatory;
	}

	@Deprecated
	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}
	@JsonIgnore
	public boolean isNew(){
		return !UtilMethods.isSet(id);

	}

	/**
	 * Returns true if this scheme is the system workflow.
	 * @return boolean
	 */
	@JsonProperty("system")
	public boolean isSystem () {
		return (SYSTEM_WORKFLOW_ID.equals(this.getId()));
	}

	public Date getModDate() {
		return modDate;
	}

	public void setModDate(Date modDate) {
		this.modDate = modDate;
	}

	
    private String entryStep = null;

    @JsonIgnore
    public String entryStep() {
        if (this.entryStep == null) {
            this.entryStep = Try.of(() -> APILocator.getWorkflowAPI().findSteps(this).get(0).getId()).getOrNull();
        }
        return this.entryStep;

    }

	@Override
	public boolean equals(final Object obj) {
		if(obj ==null || ! (obj instanceof WorkflowScheme)) {
			return false;
		}
		return ((WorkflowScheme)obj).getId().equals(this.getId());
	}

	@Override
	public int hashCode() {

		return Objects.hash(id);
	}
	
	
}
