package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;
import java.util.Date;

/**
 * @author Jonathan Gamba 2019-04-05
 */
public class WorkflowDataGen extends AbstractDataGen<WorkflowScheme> {

    private final long currentTime = System.currentTimeMillis();

    private String name = "testName" + currentTime;
    private String description = "testDescription" + currentTime;
    private Boolean archived = Boolean.FALSE;
    private Boolean defaultScheme = Boolean.FALSE;

    @SuppressWarnings("unused")
    public WorkflowDataGen name(final String name) {
        this.name = name;
        return this;
    }

    @SuppressWarnings("unused")
    public WorkflowDataGen description(final String description) {
        this.description = description;
        return this;
    }

    @SuppressWarnings("unused")
    public WorkflowDataGen archived(final Boolean archived) {
        this.archived = archived;
        return this;
    }

    @SuppressWarnings("unused")
    public WorkflowDataGen defaultScheme(final Boolean defaultScheme) {
        this.defaultScheme = defaultScheme;
        return this;
    }

    @Override
    public WorkflowScheme next() {

        final WorkflowScheme workflowScheme = new WorkflowScheme();
        workflowScheme.setName(name);
        workflowScheme.setArchived(archived);
        workflowScheme.setDescription(description);
        workflowScheme.setModDate(new Date());
        workflowScheme.setCreationDate(new Date());
        workflowScheme.setDefaultScheme(defaultScheme);

        return workflowScheme;
    }

    @Override
    public WorkflowScheme persist(final WorkflowScheme workflowScheme) {
        try {
            APILocator.getWorkflowAPI().saveScheme(workflowScheme, APILocator.systemUser());
            return workflowScheme;
        } catch (Exception e) {
            throw new RuntimeException("Unable to persist WorkflowScheme.", e);
        }
    }

    /**
     * Creates a new {@link WorkflowScheme} instance and persists it in DB
     *
     * @return A new WorkflowScheme instance persisted in DB
     */
    @Override
    public WorkflowScheme nextPersisted() {
        return persist(next());
    }

    public static void remove(final WorkflowScheme workflowScheme) {
        remove(workflowScheme, false);
    }

    public static void remove(final WorkflowScheme workflowScheme, final Boolean failSilently) {
        try {
            APILocator.getWorkflowAPI().deleteScheme(workflowScheme, APILocator.systemUser());
        } catch (Exception e) {
            if (failSilently) {
                Logger.error(ContentTypeDataGen.class, "Unable to delete WorkflowScheme.", e);
            } else {
                throw new RuntimeException("Unable to delete WorkflowScheme.", e);
            }
        }
    }

    /**
     * Creates a new {@link WorkflowScheme} instance and persists it in DB
     *
     * @return A new WorkflowScheme instance persisted in DB
     */
    @WrapInTransaction
    public WorkflowScheme nextPersistedWithStepsAndActions() {

        //First we need to create the workflow scheme
        final WorkflowScheme workflowScheme = persist(next());

        //Now the steps
        final WorkflowStep step1 = new WorkflowStepDataGen(workflowScheme.getId()).nextPersisted();
        final WorkflowStep step2 = new WorkflowStepDataGen(workflowScheme.getId()).nextPersisted();

        //Now associate actions to the steps
        final WorkflowAction action1 = new WorkflowActionDataGen(workflowScheme.getId(),
                step1.getId())
                .nextStep(step2.getId())
                .nextPersisted();
        final WorkflowAction action2 = new WorkflowActionDataGen(workflowScheme.getId(),
                step2.getId()).nextPersisted();

        //Now associate action classes to the actions
        new WorkflowActionClassDataGen(action1.getId()).nextPersisted();
        new WorkflowActionClassDataGen(action2.getId()).nextPersisted();

        return workflowScheme;
    }

}