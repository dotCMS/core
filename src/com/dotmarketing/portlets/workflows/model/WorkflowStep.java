package com.dotmarketing.portlets.workflows.model;

import java.io.Serializable;
import java.util.Date;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.dotmarketing.util.UtilMethods;

public class WorkflowStep implements Serializable {

	private static final long serialVersionUID = 1L;

	String id;
	Date creationDate = new Date();
	String name;
	String schemeId;
	int myOrder=0;
	boolean resolved;
	
	boolean enableEscalation;
	String escalationAction;
	int escalationTime;
	
	public boolean isEnableEscalation() {
        return enableEscalation;
    }
    public void setEnableEscalation(boolean enableEscalation) {
        this.enableEscalation = enableEscalation;
    }
    public String getEscalationAction() {
        return escalationAction;
    }
    public void setEscalationAction(String escalationAction) {
        this.escalationAction = escalationAction;
    }
    public int getEscalationTime() {
        return escalationTime;
    }
    public void setEscalationTime(int escalationTime) {
        this.escalationTime = escalationTime;
    }
    public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Date getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSchemeId() {
		return schemeId;
	}
	public void setSchemeId(String schemeId) {
		this.schemeId = schemeId;
	}
	public int getMyOrder() {
		return myOrder;
	}
	public void setMyOrder(int myOrder) {
		this.myOrder = myOrder;
	}
	@JsonIgnore
	public boolean isNew(){
		return !UtilMethods.isSet(id);
		
	}
	public boolean isResolved() {
		return resolved;
	}
	public void setResolved(boolean resolved) {
		this.resolved = resolved;
	}
	@Override
	public boolean equals(Object obj) {
		if(obj ==null || (!(obj instanceof WorkflowStep))) {
			return false;
		}
		
		return UtilMethods.webifyString(getId()).equals(UtilMethods.webifyString(((WorkflowStep) obj).getId()));
	}
	@Override
	public String toString() {
		return "WorkflowStep [id=" + id + ", name=" + name + ", schemeId=" + schemeId + "]";
	}
	
	
	
	
}
