package com.dotmarketing.portlets.workflows.struts;

import com.dotcms.repackage.org.apache.struts.action.ActionErrors;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotcms.repackage.org.apache.struts.validator.ValidatorForm;
import com.dotmarketing.util.InodeUtils;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.builder.ToStringBuilder;


public class WorkflowHistoryForm extends ValidatorForm
{
	
	private static final long serialVersionUID = 1L;
	
    String inode;
    Date creationDate;
    String madeBy;
    String changeDescription;
    
    
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
