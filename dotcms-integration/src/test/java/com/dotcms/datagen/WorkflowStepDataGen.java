package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import java.util.Date;

/**
 * @author Jonathan Gamba 2019-04-05
 */
public class WorkflowStepDataGen extends AbstractDataGen<WorkflowStep> {

    private final long currentTime = System.currentTimeMillis();

    private String name = "testName" + currentTime;
    private int order = 0;
    private String schemeId;
    private Boolean resolved = Boolean.FALSE;

    public WorkflowStepDataGen(String schemeId) {
        this.schemeId = schemeId;
    }

    @SuppressWarnings("unused")
    public WorkflowStepDataGen name(final String name) {
        this.name = name;
        return this;
    }

    @SuppressWarnings("unused")
    public WorkflowStepDataGen order(final int order) {
        this.order = order;
        return this;
    }

    @SuppressWarnings("unused")
    public WorkflowStepDataGen resolved(final Boolean resolved) {
        this.resolved = resolved;
        return this;
    }

    @Override
    public WorkflowStep next() {

        final WorkflowStep workflowStep = new WorkflowStep();
        workflowStep.setName(name);
        workflowStep.setMyOrder(order);
        workflowStep.setResolved(resolved);
        workflowStep.setCreationDate(new Date());
        workflowStep.setSchemeId(schemeId);

        return workflowStep;
    }

    @WrapInTransaction
    @Override
    public WorkflowStep persist(final WorkflowStep workflowStep) {
        try {
            APILocator.getWorkflowAPI().saveStep(workflowStep, APILocator.systemUser());
            return workflowStep;
        } catch (Exception e) {
            throw new RuntimeException("Unable to persist WorkflowStep.", e);
        }
    }

    /**
     * Creates a new {@link WorkflowStep} instance and persists it in DB
     *
     * @return A new WorkflowStep instance persisted in DB
     */
    @Override
    public WorkflowStep nextPersisted() {
        return persist(next());
    }

    @WrapInTransaction
    public static void remove(final WorkflowStep workflowStep) {
        try {
            APILocator.getWorkflowAPI().deleteStep(workflowStep, APILocator.systemUser());
        } catch (Exception e) {
            throw new RuntimeException("Unable to delete WorkflowStep.", e);
        }
    }

}