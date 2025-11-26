package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.*;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import org.apache.velocity.context.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Actionlet(comment = true)
public class CommentOnWorkflowActionlet extends WorkFlowActionlet {


    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static List<WorkflowActionletParameter> paramList = null;

    @Override
    public synchronized List<WorkflowActionletParameter> getParameters() {
        if (paramList == null) {
            paramList = new ArrayList<>();
            paramList.add(new WorkflowActionletParameter("comment", "Workflow Comment", null, true));
        }
        return paramList;
    }

    public String getName() {

        String name = "Comment on Workflow";

        try {
            name = LanguageUtil.get("Workflow-Comment-on-workflow");
        } catch (LanguageException e) {
            name = "Comment on Workflow";
        }

        return name;
    }

    public String getHowTo() {

        String name = "This actionlet allows you to add a comment on the workflow task.";

        try {
            name = LanguageUtil.get("Workflow-Comment-on-workflow-howto");
        } catch (LanguageException e) {
            name = "This actionlet allows you to add a comment on the workflow task.";
        }

        return name;
    }

    public void executeAction(final WorkflowProcessor processor,
                              final Map<String, WorkflowActionClassParameter> params)
            throws WorkflowActionFailureException {

        final WorkflowActionClassParameter commentParam = params.get("comment");
        final WorkflowComment              comment      = new WorkflowComment();
        final Contentlet                   contentlet   = processor.getContentlet();

        this.setRole(processor, comment);
        this.processCommentValue(processor, commentParam, comment);

        if (null != processor.getTask()) { // if the content is new the

            comment.setWorkflowtaskId(processor.getTask().getId());
        }

        if (null == processor.getTask() || processor.getTask().isNew()) {
            try {
                HibernateUtil.addCommitListener(() -> {

                    if (null != processor.getTask()) {
                        if (!UtilMethods.isSet(comment.getWorkflowtaskId())) {

                            comment.setWorkflowtaskId(processor.getTask().getId());
                        }

                        this.saveComment(contentlet, comment);
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

    private void setRole(final WorkflowProcessor processor,
                         final WorkflowComment comment) {
        try {
            comment.setPostedBy(processor.getUser().getUserId());
        } catch (Exception e1) {
            throw new WorkflowActionFailureException("unable to load role:" + processor.getUser(), e1);
        }
    }

    private void processCommentValue(final WorkflowProcessor processor,
                                     final WorkflowActionClassParameter commentParam,
                                     final WorkflowComment comment) {

        final Context velocityContext = VelocityUtil.getBasicContext();
        velocityContext.put("workflow", processor);
        velocityContext.put("user", processor.getUser());
        velocityContext.put("contentlet", processor.getContentlet());
        velocityContext.put("content", processor.getContentlet());

        try {
            comment.setComment(VelocityUtil.eval(commentParam.getValue(), velocityContext));
        } catch (Exception e1) {
            Logger.warn(this.getClass(), "unable to parse comment, falling back" + e1);
            comment.setComment(commentParam.getValue());
        }
    }

    @WrapInTransaction
    private void saveComment (final Contentlet contentlet, final WorkflowComment comment) {

        try {
            final WorkflowTask task = APILocator.getWorkflowAPI().findTaskByContentlet(contentlet);
            comment.setWorkflowtaskId(task.getId());
            APILocator.getWorkflowAPI().saveComment(comment);
        } catch (Exception e) {
            Logger.warn(CommentOnWorkflowActionlet.class, "unable to save comment");
        }
    }

    public WorkflowStep getNextStep() {
        return null;
    }

    
    
    
    
}
