package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import java.util.List;
import java.util.Map;

/**
 * Actionlet to destroy a contentlet
 * @author jsanca
 */
@Actionlet(destroy = true)
public class DestroyContentActionlet extends WorkFlowActionlet {
    
    private static final long serialVersionUID = -2314685590620626801L;

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        return null;
    }
    
    @Override
    public String getName() {
        return "Destroy content";
    }
    
    @Override
    public String getHowTo() {
        return "This will destroy the content in ALL languages that exist and cannot be undone";
    }
    
    @Override
    public void executeAction(final WorkflowProcessor processor,
            final Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

        try {

            APILocator.getContentletAPI().destroy(processor.getContentlet(), processor.getUser(), false);
            processor.setTask(null);
            processor.setContentlet(null);
            processor.abortProcessor();
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            throw new WorkflowActionFailureException(e.getMessage(), e);

        }
    }
    
    @Override
    public boolean stopProcessing() {
        return true;
    }
}
