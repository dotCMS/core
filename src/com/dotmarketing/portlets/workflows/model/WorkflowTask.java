package com.dotmarketing.portlets.workflows.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.dotmarketing.util.UtilMethods;


public class WorkflowTask  implements Serializable
{
	
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


    public String getWebasset() {
        return webasset;
    }

    public void setWebasset(String webasset) {
        this.webasset = webasset;
    }

	@Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }


    public Map getMap () {
        Map oMap = new HashMap ();
        oMap.put("assignedTo", this.getAssignedTo());
        oMap.put("belongsTo", this.getBelongsTo());
        oMap.put("createdBy", this.getCreatedBy());
        oMap.put("cretionDate", this.getCreationDate());
        oMap.put("description", this.getDescription());
        oMap.put("dueDate", this.getDueDate());
        oMap.put("modDate", this.getModDate());
        oMap.put("title", this.getTitle());
        oMap.put("status", this.getStatus());
        oMap.put("id",this.id);
        return oMap;
    }
	public boolean isNew(){
		return !UtilMethods.isSet(id);
		
	}
	@Override
	public boolean equals(Object obj) {
		if(obj ==null || ! (obj instanceof WorkflowTask)) return false;
		return ((WorkflowTask)obj).getId().equals(this.getId());
	}
}
