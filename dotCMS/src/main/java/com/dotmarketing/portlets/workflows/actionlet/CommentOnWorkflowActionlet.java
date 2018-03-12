package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.VelocityUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommentOnWorkflowActionlet extends WorkFlowActionlet {


    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static List<WorkflowActionletParameter> paramList = null;

    @Override
    public synchronized List<WorkflowActionletParameter> getParameters() {
        if (paramList == null) {
            paramList = new ArrayList<WorkflowActionletParameter>();
            paramList.add(new WorkflowActionletParameter("comment", "Workflow Comment", null, true));
        }
        return paramList;
    }

    public String getName() {
        return "Comment on Workflow";
    }

    public String getHowTo() {

        return "This actionlet allows you to add a comment on the workflow task.";
    }

    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
            throws WorkflowActionFailureException {
        WorkflowActionClassParameter commentParam = params.get("comment");
        WorkflowComment comment = new WorkflowComment();
        final Contentlet con = processor.getContentlet();
        Role role;
        try {
            role = APILocator.getRoleAPI().getUserRole(processor.getUser());
            comment.setPostedBy(role.getId());
        } catch (DotDataException e1) {
            throw new WorkflowActionFailureException("unable to load role:" + processor.getUser(), e1);
        }


        org.apache.velocity.context.Context ctx = VelocityUtil.getBasicContext();
        ctx.put("workflow", processor);
        ctx.put("user", processor.getUser());
        ctx.put("contentlet", processor.getContentlet());
        ctx.put("content", processor.getContentlet());

        try {
            comment.setComment(VelocityUtil.eval(commentParam.getValue(), ctx));
        } catch (Exception e1) {
            Logger.warn(this.getClass(), "unable to parse comment, falling back" + e1);
            comment.setComment(commentParam.getValue());
        }

        comment.setWorkflowtaskId(processor.getTask().getId());

        if (processor.getTask().isNew()) {
            try {
                HibernateUtil.addAsyncCommitListener(() -> {
                    try {
                        WorkflowTask task = APILocator.getWorkflowAPI().findTaskByContentlet(con);
                        comment.setWorkflowtaskId(task.getId());
                        APILocator.getWorkflowAPI().saveComment(comment);
                        DbConnectionFactory.closeAndCommit();
                    } catch (Exception e) {
                        Logger.warn(CommentOnWorkflowActionlet.class, "unable to save comment");
                    }
                });
            } catch (DotHibernateException e1) {

                Logger.warn(CommentOnWorkflowActionlet.class, e1.getMessage());
            }
        } else {
            try {
                APILocator.getWorkflowAPI().saveComment(comment);
            } catch (DotDataException e) {
                Logger.error(CommentOnWorkflowActionlet.class, e.getMessage(), e);
            }
        }

    }

    public WorkflowStep getNextStep() {
        return null;
    }

    
    
    
    
}
