package com.dotmarketing.portlets.workflows.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.dotmarketing.util.UtilMethods;


public class WorkflowComment  implements Serializable
{
	
	private static final long serialVersionUID = 1L;
	
	String id;
    Date creationDate;
    String postedBy;
    String comment;
    String workflowtaskId;

    public WorkflowComment(){
    	creationDate = new Date();
    }
    
    public Date getCreationDate() {
        return creationDate;
    }


    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }


    public String getComment() {
        return comment;
    }


    public void setComment(String comment) {
        this.comment = comment;
    }


    public String getPostedBy() {
        return postedBy;
    }


    public void setPostedBy(String postedBy) {
        this.postedBy = postedBy;
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
        oMap.put("comment", this.getComment());
        oMap.put("creationDate", this.getCreationDate());
        oMap.put("postedBy", this.getPostedBy());
        return oMap;
    }
	public boolean isNew(){
		return UtilMethods.isSet(id);
		
	}
}
