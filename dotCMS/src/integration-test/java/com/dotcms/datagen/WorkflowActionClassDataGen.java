package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;

/**
 * @author Jonathan Gamba 2019-04-05
 */
public class WorkflowActionClassDataGen extends AbstractDataGen<WorkflowActionClass> {

    private String actionId;
    private Class actionClass = SaveContentActionlet.class;
    private int order = 0;

    public WorkflowActionClassDataGen(String actionId) {
        this.actionId = actionId;
    }

    @SuppressWarnings("unused")
    public WorkflowActionClassDataGen actionClass(final Class actionClass) {
        this.actionClass = actionClass;
        return this;
    }

    @SuppressWarnings("unused")
    public WorkflowActionClassDataGen order(final int order) {
        this.order = order;
        return this;
    }

    @Override
    public WorkflowActionClass next() {

        final WorkflowActionClass workflowActionClass;
        try {
            workflowActionClass = new WorkflowActionClass();
            workflowActionClass.setActionId(actionId);
            workflowActionClass.setClazz(actionClass.getName());
            workflowActionClass
                    .setName(WorkFlowActionlet.class.cast(actionClass.newInstance()).getName());
            workflowActionClass.setOrder(order);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create WorkflowActionClass instance.", e);
        }

        return workflowActionClass;
    }

    @WrapInTransaction
    @Override
    public WorkflowActionClass persist(final WorkflowActionClass workflowActionClass) {
        try {
            APILocator.getWorkflowAPI()
                    .saveActionClass(workflowActionClass, APILocator.systemUser());
            return workflowActionClass;
        } catch (Exception e) {
            throw new RuntimeException("Unable to persist WorkflowActionClass.", e);
        }
    }

    /**
     * Creates a new {@link WorkflowActionClass} instance and persists it in DB
     *
     * @return A new WorkflowActionClass instance persisted in DB
     */
    @Override
    public WorkflowActionClass nextPersisted() {
        return persist(next());
    }

    @WrapInTransaction
    public static void remove(final WorkflowActionClass workflowActionClass) {
        try {
            APILocator.getWorkflowAPI()
                    .deleteActionClass(workflowActionClass, APILocator.systemUser());
        } catch (Exception e) {
            throw new RuntimeException("Unable to delete WorkflowActionClass.", e);
        }
    }

}