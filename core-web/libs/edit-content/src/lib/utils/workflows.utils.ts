import { DotCMSWorkflow, DotCMSWorkflowAction, WorkflowStep } from '@dotcms/dotcms-models';

import { CurrentContentActionsWithScheme } from '../models/dot-edit-content-field.type';

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
 * Parses current workflow actions into a map of scheme ID to actions
 *
 * @param actions Array of workflow actions
 * @returns CurrentContentActionsWithScheme - Record of scheme IDs mapped to their corresponding actions
 */
export const parseCurrentActions = (
    actions: DotCMSWorkflowAction[]
): CurrentContentActionsWithScheme => {
    if (!Array.isArray(actions)) {
        return {};
    }

    return actions.reduce((acc, action) => {
        const { schemeId } = action;

        if (!acc[schemeId]) {
            acc[schemeId] = [];
        }

        acc[schemeId].push(action);

        return acc;
    }, {} as CurrentContentActionsWithScheme);
};
