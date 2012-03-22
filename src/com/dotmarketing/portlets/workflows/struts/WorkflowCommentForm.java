package com.dotmarketing.portlets.workflows.struts;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.util.InodeUtils;



public class WorkflowCommentForm extends ValidatorForm 
{
	
	private static final long serialVersionUID = 1L;
	
    String inode;
    Date creationDate;
    String postedBy;
    String comment;

    
    public String getInode() {
    	if(InodeUtils.isSet(inode))
			return inode;
		
		return "";
    }


    public void setInode(String inode) {
        this.inode = inode;
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


    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public ActionErrors validate(ActionMapping arg0, HttpServletRequest arg1) {
        ActionErrors ae = new ActionErrors();   
        ae = super.validate(arg0,arg1);
        return ae;
    }

}
