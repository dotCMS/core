package com.dotmarketing.portlets.workflows.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.dotmarketing.util.UtilMethods;

public class WorkflowHistory  implements Serializable
{
	
	private static final long serialVersionUID = 1L;
	
	String id;
    Date creationDate;
    String madeBy;
    String changeDescription;
    String workflowtaskId;
    String actionId;
    String stepId;
    
    public String getStepId() {
		return stepId;
	}

	public void setStepId(String stepId) {
		this.stepId = stepId;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String getActionId() {
		return actionId;
	}

	public void setActionId(String actionId) {
		this.actionId = actionId;
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

	public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getChangeDescription() {
        return changeDescription;
    }

    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }

    public String getMadeBy() {
        return madeBy;
    }

    public void setMadeBy(String madeBy) {
        this.madeBy = madeBy;
    }
    
    public String getWorkflowtaskId() {
		return workflowtaskId;
	}

	public void setWorkflowtaskId(String workflowtaskId) {
		this.workflowtaskId = workflowtaskId;
	}

	@Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    
    public Map getMap () {
        Map oMap = new HashMap ();
        oMap.put("creationDate", this.getCreationDate());
        oMap.put("madeBy", this.getMadeBy());
        oMap.put("changeDescription", this.getChangeDescription());
        oMap.put("workflowTaskId",this.workflowtaskId);
        return oMap;
    }
	public boolean isNew(){
		return UtilMethods.isSet(id);
		
	}
}
