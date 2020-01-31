package com.dotmarketing.portlets.workflows.model;

import com.dotcms.util.marshal.MarshalFactory;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.StringPool;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class WorkflowHistory  implements Serializable, WorkflowTimelineItem
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

        if (UtilMethods.isSet(this.changeDescription)  && StringUtils.isJson(this.changeDescription.trim())) {
            // if it is json
            return(String) this.getChangeMap().get("description");
        }

        // if the representation of the json, if it is  json. otherwise is gonna be description, getChangeDescription
        return this.changeDescription;
    }

    public String getRawChangeDescription() {
        return this.changeDescription;
    }

    public Map<String, Object> getChangeMap () {

        if (UtilMethods.isSet(this.changeDescription) && StringUtils.isJson(this.changeDescription.trim())) {
            // if it is json
            return MarshalFactory.getInstance().getMarshalUtils().unmarshal(this.changeDescription, Map.class);
        }

        // if the representation of the json, if it is  json. otherwise is gonna be description, getChangeDescription
        return ImmutableMap.of("description", null == this.changeDescription? StringPool.BLANK:this.changeDescription,
                "type", WorkflowHistoryType.COMMENT.name(), "state", WorkflowHistoryState.NONE.name());
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

    @Override
    public Date createdDate() {
      
        return this.getCreationDate();
    }

    @Override
    public String roleId() {
       
        return this.getMadeBy();
    }

    @Override
    public String actionId() {
      
        return this.getActionId();
    }

    @Override
    public String stepId() {
        
        return this.getStepId();
    }

    @Override
    public String commentDescription() {
     
        return this.getChangeDescription();
    }

    @Override
    public String taskId() {
        return this.getWorkflowtaskId();
    }

    @Override
    public String type() {
        return this.getClass().getSimpleName();
    }
}
