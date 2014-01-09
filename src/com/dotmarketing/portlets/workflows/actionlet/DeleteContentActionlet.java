package com.dotmarketing.portlets.workflows.actionlet;

import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;

public class DeleteContentActionlet extends ContentActionlet {
    
    private static final long serialVersionUID = -2314685590620626801L;

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        return null;
    }
    
    @Override
    public String getName() {
        return "Delete content";
    }
    
    @Override
    public String getHowTo() {
        return "This action will delete the content. Warning: this can't be undone!";
    }
    
    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {
        try {
        	super.executeAction(processor, params);
           	for(Contentlet c:contentletsToProcess){
           		if(!processor.getContentlet().isArchived())
           			APILocator.getContentletAPI().archive(c, processor.getUser(), false);
           		APILocator.getContentletAPI().delete(c, processor.getUser(), false);
           	}            	
            processor.setTask(null);
            processor.setContentlet(null);
        } catch (Exception e) {
            Logger.error(this.getClass(),e.getMessage(),e);
            throw new  WorkflowActionFailureException(e.getMessage());
        
        }
    }
    
    @Override
    public boolean stopProcessing() {
        return true;
    }
}
