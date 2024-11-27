import { DotCMSWorkflow, DotCMSWorkflowAction, WorkflowStep } from '@dotcms/dotcms-models';

import { ContentState } from '../feature/edit-content/store/features/content.feature';

/**
 * Parses an array of workflow data and returns a new object with key-value pairs.
 *
 * @param {Object[]} data - The array of workflow data to be parsed.
 * @returns {Object} - The parsed object with key-value pairs.
 */
export const parseWorkflows = (
    data: {
        scheme: DotCMSWorkflow;
        action: DotCMSWorkflowAction;
        firstStep: WorkflowStep;
    }[]
) => {
    if (!Array.isArray(data)) {
        return {};
    }

    return data.reduce((acc, { scheme, action, firstStep }) => {
        if (!acc[scheme.id]) {
            acc[scheme.id] = {
                scheme: {
                    ...scheme
                },
                actions: [],
                firstStep
            };
        }

        acc[scheme.id].actions.push(action);

        return acc;
    }, {});
};

/**
 * Determines if workflow action buttons should be shown based on content and scheme state
 * Shows workflow buttons when:
 * - Content type has only one workflow scheme OR
 * - Content is existing AND has a selected workflow scheme OR
 * - Content is new and has selected a workflow scheme
 *
 * @param schemes - Available workflow schemes object
 * @param contentlet - Current contentlet (if exists)
 * @param currentSchemeId - Selected workflow scheme ID
 * @returns boolean indicating if workflow actions should be shown
 */
export const shouldShowWorkflowActions = ({
    schemes,
    contentlet,
    currentSchemeId,
    step
}: {
    schemes: ContentState['schemes'];
    contentlet: ContentState['contentlet'];
    currentSchemeId: string | null;
    step: WorkflowStep | null;
}): boolean => {
    // No step means no workflow actions
    if (!step) {
        return false;
    }

    const hasOneScheme = Object.keys(schemes).length === 1;
    const isExisting = !!contentlet;
    const hasSelectedScheme = !!currentSchemeId;

    if (hasOneScheme) {
        return true;
    }

    if (isExisting && hasSelectedScheme) {
        return true;
    }

    if (!isExisting && hasSelectedScheme) {
        return true;
    }

    return false;
};

/**
 * Determines if workflow selection warning should be shown
 * Shows warning when:
 * - Content is new (no contentlet exists) AND
 * - Content type has multiple workflow schemes AND
 * - No workflow scheme has been selected
 *
 * @param schemes - Available workflow schemes object
 * @param contentlet - Current contentlet (if exists)
 * @param currentSchemeId - Selected workflow scheme ID
 * @returns boolean indicating if workflow selection warning should be shown
 */
export const shouldShowWorkflowWarning = ({
    schemes,
    contentlet,
    currentSchemeId
}: {
    schemes: ContentState['schemes'];
    contentlet: ContentState['contentlet'];
    currentSchemeId: string | null;
}): boolean => {
    const isNew = !contentlet;
    const hasNoSchemeSelected = !currentSchemeId;
    const hasMultipleSchemas = Object.keys(schemes).length > 1;

    return isNew && hasMultipleSchemas && hasNoSchemeSelected;
};

/**
 * Gets the appropriate workflow actions based on content state
 * Returns:
 * - Empty array if no scheme is selected
 * - Current content actions for existing content
 * - Sorted scheme actions for new content (with 'Save' action first)
 *
 * @param schemes - Available workflow schemes object
 * @param contentlet - Current contentlet (if exists)
 * @param currentSchemeId - Selected workflow scheme ID
 * @param currentContentActions - Current content specific actions
 * @returns Array of workflow actions
 */
export const getWorkflowActions = ({
    schemes,
    contentlet,
    currentSchemeId,
    currentContentActions
}: {
    schemes: ContentState['schemes'];
    contentlet: ContentState['contentlet'];
    currentSchemeId: string | null;
    currentContentActions: DotCMSWorkflowAction[];
}): DotCMSWorkflowAction[] => {
    const isNew = !contentlet;

    if (!currentSchemeId || !schemes[currentSchemeId]) {
        return [];
    }

    if (!isNew && currentContentActions.length) {
        return currentContentActions;
    }

    return Object.values(schemes[currentSchemeId].actions).sort((a, b) => {
        if (a.name === 'Save') return -1;
        if (b.name === 'Save') return 1;

        return a.name.localeCompare(b.name);
    });
};
