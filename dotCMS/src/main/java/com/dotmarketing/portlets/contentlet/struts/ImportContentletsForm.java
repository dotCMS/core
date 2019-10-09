package com.dotmarketing.portlets.contentlet.struts;


import com.dotcms.repackage.org.apache.struts.action.ActionErrors;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotcms.repackage.org.apache.struts.validator.ValidatorForm;
import javax.servlet.http.HttpServletRequest;

public class ImportContentletsForm extends ValidatorForm {

    private static final long serialVersionUID = 1L;

	/** identifier field */    
	
	private String structure = "";
	
	private String fileName = "";
	
	private long language = 0;
	
	private String[] fields = new String[0];

	private String workflowActionId = "";

    public String[] getFields() {
        return fields;
    }

    public void setFields(String[] fields) {
        this.fields = fields;
    }

    public String getStructure() {
        return structure;
    }

    public void setStructure(String structure) {
        this.structure = structure;
    }

    public long getLanguage() {
        return language;
    }

    public void setLanguage(long language) {
        this.language = language;
    }

    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest req) {
        
        return super.validate(mapping, req);
    }

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

    /**
     * Set the WorkflowActionId to be executed on the import
     * @param workflowActionId the workflow action Id string
     */
    public void setWorkflowActionId(String workflowActionId) {
        this.workflowActionId = workflowActionId;
    }

    /**
     * Get the Workflow Action Id to be executed on the import
     * @return The workflow Action Id
     */
    public String getWorkflowActionId() {
        return workflowActionId;
    }
    
    
    
}
