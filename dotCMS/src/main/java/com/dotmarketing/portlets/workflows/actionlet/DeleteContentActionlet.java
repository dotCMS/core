package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.language.LanguageUtil;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;

/**
 * Deletes a single version of the contentlet.
 */
@Actionlet(delete = true)
public class DeleteContentActionlet extends WorkFlowActionlet {
    
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
    public void executeAction(final WorkflowProcessor processor, final Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

        try {

            processor.getContentlet().setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE);
            if (processor.getContentlet().isHTMLPage()) {

                final int relatedContentTypes = APILocator.getContentTypeAPI(processor.getUser())
                        .count("page_detail='" + processor.getContentlet().getIdentifier() + "'");
                if (relatedContentTypes > 0) {

                    throw new WorkflowActionFailureException(LanguageUtil.get(processor.getUser(),
                            "HTML-Page-related-content-type-delete-error"));
                }

            }

            processor.getContextMap().put("deleted", APILocator.getContentletAPI()
                    .delete(processor.getContentlet(), processor.getUser(), false));
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
