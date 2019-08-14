package com.dotmarketing.portlets.workflows.struts;

import com.dotcms.repackage.org.apache.struts.action.ActionErrors;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotcms.repackage.org.apache.struts.validator.ValidatorForm;
import com.dotmarketing.util.InodeUtils;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.builder.ToStringBuilder;



public class WorkflowTaskForm extends ValidatorForm
{
	
	private static final long serialVersionUID = 1L;
	
    String inode;
    Date creationDate;
    Date modDate;
    Date dueDate;
    String dueDateMonth;
    String dueDateDay;
    String dueDateYear;
    boolean noDueDate;
    String createdBy;
    String assignedTo;
    String belongsTo;
    String title;
    String description;
    String status;
    String webasset = "";

        
    public String getInode() {
    	if(InodeUtils.isSet(inode))
			return inode;
		
		return "";
    }


    public void setInode(String inode) {
        this.inode = inode;
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

   

    public String getDueDateDay() {
        return dueDateDay;
    }


    public void setDueDateDay(String dueDateDay) {
        this.dueDateDay = dueDateDay;
    }


    public String getDueDateMonth() {
        return dueDateMonth;
    }


    public void setDueDateMonth(String dueDateMonth) {
        this.dueDateMonth = dueDateMonth;
    }


    public String getDueDateYear() {
        return dueDateYear;
    }


    public void setDueDateYear(String dueDateYear) {
        this.dueDateYear = dueDateYear;
    }


    public boolean isNoDueDate() {
        return noDueDate;
    }


    public void setNoDueDate(boolean noDueDate) {
        this.noDueDate = noDueDate;
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

    public ActionErrors validate(ActionMapping arg0, HttpServletRequest arg1) {
        ActionErrors ae = new ActionErrors();   
        ae = super.validate(arg0,arg1);
        return ae;
    }
}
