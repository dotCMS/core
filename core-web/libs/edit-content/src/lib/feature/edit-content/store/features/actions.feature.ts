import { signalStoreFeature, type, withComputed, withState } from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotCMSWorkflowAction, WorkflowStep } from '@dotcms/dotcms-models';

import { ContentState } from './content.feature';

import { EditContentRootState } from '../edit-content.store';

export interface ActionsState {
    /** Current workflow step */
    currentStep: WorkflowStep | null;
    /** Actions available for the current content */
    currentContentActions: DotCMSWorkflowAction[];
}

const actionsInitialState: ActionsState = {
    currentStep: null,
    currentContentActions: []
};

export function withActions() {
    return signalStoreFeature(
        { state: type<EditContentRootState & ContentState>() },
        withState(actionsInitialState),
        withComputed((store) => ({
            /**
             * Computed property that retrieves the current step of the workflow.
             * Returns the first step of the selected scheme when:
             * 1. Content is new (no contentlet)
             * 2. Content exists but workflow was reset (no current step)
             * Otherwise returns the current step from the workflow status.
             *
             * @returns {WorkflowStep} The current workflow step
             */
            getCurrentStep: computed(() => {
                const currentStep = store.currentStep();

                if (!currentStep) {
                    // No step assigned (new content or reset workflow)
                    const schemes = store.schemes();
                    const currentSchemeId = store.currentSchemeId();

                    return schemes[currentSchemeId]?.firstStep;
                }

                // Existing content with current step
                return currentStep;
            })
        }))
    );
}
