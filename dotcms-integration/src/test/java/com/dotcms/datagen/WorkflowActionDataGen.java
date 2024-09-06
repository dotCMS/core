package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.util.UtilMethods;
import java.util.Set;

/**
 * @author Jonathan Gamba 2019-04-05
 */
public class WorkflowActionDataGen extends AbstractDataGen<WorkflowAction> {

    private final long currentTime = System.currentTimeMillis();

    private String name = "testName" + currentTime;
    private String schemeId;
    private String stepId;
    private String nextStep;
    private Set<WorkflowState> showOn = WorkflowAPI.DEFAULT_SHOW_ON;
    private String condition;
    private String nextAssign;
    private int order = 0;
    private Boolean commentable = Boolean.FALSE;
    private Boolean assignable = Boolean.FALSE;

    public WorkflowActionDataGen(String schemeId, String stepId) {
        this.schemeId = schemeId;
        this.stepId = stepId;
        this.nextStep = stepId;
        try {
            this.nextAssign = APILocator.getRoleAPI().loadCMSAnonymousRole().getId();
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    public WorkflowActionDataGen name(final String name) {
        this.name = name;
        return this;
    }

    @SuppressWarnings("unused")
    public WorkflowActionDataGen nextStep(final String nextStep) {
        this.nextStep = nextStep;
        return this;
    }

    @SuppressWarnings("unused")
    public WorkflowActionDataGen showOn(final Set<WorkflowState> showOn) {
        this.showOn = showOn;
        return this;
    }

    @SuppressWarnings("unused")
    public WorkflowActionDataGen condition(final String condition) {
        this.condition = condition;
        return this;
    }

    @SuppressWarnings("unused")
    public WorkflowActionDataGen nextAssign(final String nextAssign) {
        this.nextAssign = nextAssign;
        return this;
    }

    @SuppressWarnings("unused")
    public WorkflowActionDataGen order(final int order) {
        this.order = order;
        return this;
    }

    @SuppressWarnings("unused")
    public WorkflowActionDataGen commentable(final Boolean commentable) {
        this.commentable = commentable;
        return this;
    }

    @SuppressWarnings("unused")
    public WorkflowActionDataGen assignable(final Boolean assignable) {
        this.assignable = assignable;
        return this;
    }

    @Override
    public WorkflowAction next() {

        final WorkflowAction workflowAction = new WorkflowAction();
        workflowAction.setName(name);
        workflowAction.setSchemeId(schemeId);
        workflowAction.setNextStep(nextStep);
        workflowAction.setShowOn(showOn);
        workflowAction.setRequiresCheckout(false);
        workflowAction.setCondition(condition);
        workflowAction.setNextAssign(nextAssign);
        workflowAction.setOrder(order);
        workflowAction.setCommentable(commentable);
        workflowAction.setAssignable(assignable);

        return workflowAction;
    }

    @WrapInTransaction
    @Override
    public WorkflowAction persist(final WorkflowAction workflowAction) {
        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();

        try {
            if(!UtilMethods.isSet(workflowAction.getId())){
                workflowAPI.saveAction(workflowAction, null, APILocator.systemUser());
            }
            workflowAPI.saveAction(workflowAction.getId(), stepId, APILocator.systemUser());
            return workflowAction;
        } catch (Exception e) {
            throw new RuntimeException("Unable to persist WorkflowAction.", e);
        }
    }

    /**
     * Creates a new {@link WorkflowAction} instance and persists it in DB
     *
     * @return A new WorkflowAction instance persisted in DB
     */
    @Override
    public WorkflowAction nextPersisted() {
        return persist(next());
    }

    @WrapInTransaction
    public static void remove(final WorkflowAction workflowAction) {
        try {
            APILocator.getWorkflowAPI().deleteAction(workflowAction, APILocator.systemUser());
        } catch (Exception e) {
            throw new RuntimeException("Unable to delete WorkflowAction.", e);
        }
    }

}