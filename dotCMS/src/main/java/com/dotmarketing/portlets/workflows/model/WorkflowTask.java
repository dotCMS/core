package com.dotmarketing.portlets.workflows.model;

import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a Workflow Task that is part of a Workflow in dotCMS.
 * <p>A Workflow Tasks is a content item which has been assigned to a specific user or a Role,
 * indicating that the user or some member of the Role needs to take action on the content
 * item.</p>
 * <p>The Workflow Task contains links to edit the item, the history of all Workflow Actions taken
 * on the content (including the Actions taken, users that took them, and timestamp), and comments
 * that have been written about this piece of content as the content has progressed through the
 * Workflow. Workflow Tasks also allow you to attach files to the content (such as specifications or
 * external communications about the content), so that those files follow the content through the
 * Workflow.</p>
 *
 * @author root
 * @since Mar 22nd, 2012
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowTask implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	String id;
    Date creationDate;
    Date modDate;
    Date dueDate;
    String createdBy;
    String assignedTo;
    String belongsTo;
    String title;
    String description;
    String status;
    String webasset;
    long languageId;
    
    public WorkflowTask(){
    	creationDate = new Date();
    	modDate = new Date();
    	
    }
    
    public String getId() {
		return id;
	}
    
	public void setId(String id) {
		this.id = id;
	}
	
	public void setInode(String id){
		setId(id);
	}

	public String getInode(){
		return id;
	}
	
	public String getAssignedTo() {
        return assignedTo;
    }


    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }


    public String getBelongsTo() {
        return belongsTo;
    }


    public void setBelongsTo(String belongsTo) {
        this.belongsTo = belongsTo;
    }


    public String getCreatedBy() {
        return createdBy;
    }


    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }


    public Date getCreationDate() {
        return creationDate;
    }


    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }


    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public Date getDueDate() {
        return dueDate;
    }


    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }


    public Date getModDate() {
        return modDate;
    }


    public void setModDate(Date modDate) {
        this.modDate = modDate;
    }

    public String getStatus() {
        return status;
    }


    public void setStatus(String status) {
        this.status = status;
    }


    public String getTitle() {
        return title;
    }


    public void setTitle(String title) {
        this.title = title;
    }

    public long getLanguageId() {
        return languageId;
    }

    public void setLanguageId(final long languageId) {
        this.languageId = languageId;
    }

    public String getWebasset() {
        return webasset;
    }

    public void setWebasset(String webasset) {
        this.webasset = webasset;
    }


    @Override
    public String toString() {
        return "WorkflowTask{" +
                "id='" + id + '\'' +
                ", creationDate=" + creationDate +
                ", modDate=" + modDate +
                ", dueDate=" + dueDate +
                ", createdBy='" + createdBy + '\'' +
                ", assignedTo='" + assignedTo + '\'' +
                ", belongsTo='" + belongsTo + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", webasset='" + webasset + '\'' +
                ", languageId=" + languageId +
                '}';
    }

    /**
     * Returns a data Map with all the properties from this Workflow Task.
     *
     * @return A {@link Map} with this task's properties.
     */
    @JsonIgnore
    public Map<String, Object> getMap () {
        Map<String, Object> oMap = new HashMap<>();
        oMap.put("assignedTo", this.getAssignedTo());
        oMap.put("belongsTo", this.getBelongsTo());
        oMap.put("createdBy", this.getCreatedBy());
        oMap.put("creationDate", this.getCreationDate());
        oMap.put("description", this.getDescription());
        oMap.put("dueDate", this.getDueDate());
        oMap.put("modDate", this.getModDate());
        oMap.put("title", this.getTitle());
        oMap.put("status", this.getStatus());
        oMap.put("languageId", this.getLanguageId());
        oMap.put("id",this.id);
        oMap.put("webasset",this.id);
        return oMap;
    }

	public boolean isNew(){
		return !UtilMethods.isSet(id);
		
	}
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof WorkflowTask)) return false;
		return ((WorkflowTask)obj).getId().equals(this.getId());
	}

}
