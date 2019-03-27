package com.dotcms.rest.api.v1.workflow;

import com.dotmarketing.portlets.workflows.model.WorkflowScheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BulkActionView {

    private final List<BulkWorkflowSchemeView> schemes;

    public BulkActionView(final Map<WorkflowScheme, Map<CountWorkflowStep, List<CountWorkflowAction>>> bulkActions) {

        this.schemes = new ArrayList<>();

        for (final Map.Entry<WorkflowScheme, Map<CountWorkflowStep, List<CountWorkflowAction>>> entry : bulkActions.entrySet()) {

            final BulkWorkflowSchemeView bulkWorkflowSchemeView =
                    new BulkWorkflowSchemeView(entry.getKey(), this.getSteps (entry.getValue()));
            this.schemes.add(bulkWorkflowSchemeView);
        }
    }

    private List<BulkWorkflowStepView> getSteps(final Map<CountWorkflowStep, List<CountWorkflowAction>> stepListMap) {

        final List<BulkWorkflowStepView> steps = new ArrayList<>();

        for (final Map.Entry<CountWorkflowStep, List<CountWorkflowAction>> entry : stepListMap.entrySet()) {

            final BulkWorkflowStepView stepView = new BulkWorkflowStepView(entry.getKey(), entry.getValue());
            steps.add(stepView);
        }

        return steps;
    }

    public List<BulkWorkflowSchemeView> getSchemes() {
        return schemes;
    }
}
