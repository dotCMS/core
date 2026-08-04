import { DotCMSWorkflowAction } from './dot-workflow-action.model';

/**
 * Request body for `POST /api/v1/workflow/contentlet/actions/bulk`.
 *
 * Either `contentletIds` or `query` must be supplied. Note that despite the property name,
 * `contentletIds` holds contentlet **inodes**, not identifiers — the backend documents this
 * explicitly on the endpoint.
 */
export interface DotBulkActionRequest {
    /** Contentlet **inodes** (not identifiers, despite the name). */
    contentletIds?: string[];
    /** Lucene query, used when the selection spans pages ("select all"). */
    query?: string;
}

/**
 * A workflow step with the number of selected contentlets currently sitting in it.
 */
export interface DotCountWorkflowStep {
    count: number;
    workflowStep: {
        id: string;
        name: string;
        schemeId: string;
    };
}

/**
 * A workflow action with the number of selected contentlets it applies to.
 *
 * `count` is the number of selected contentlets currently in a step that exposes this action.
 * It is already summed across every step of the scheme by the backend, so flattening
 * `steps[] -> actions[]` does not double-count.
 *
 * `count` is an **upper bound** when `conditionPresent` is true: the backend does not evaluate
 * the action's Velocity condition when aggregating, because there is no per-contentlet
 * permissionable at that point.
 */
export interface DotCountWorkflowAction {
    count: number;
    workflowAction: DotCMSWorkflowAction;
    /** Action has a push-publish actionlet — needs environment/date inputs before firing. */
    pushPublish: boolean;
    /** Action has a move actionlet with no path — needs a target path before firing. */
    moveable: boolean;
    /** Action has a Velocity condition that was NOT evaluated when computing `count`. */
    conditionPresent: boolean;
}

/**
 * One workflow scheme and its steps, as returned inside {@link DotBulkActionView}.
 */
export interface DotBulkWorkflowSchemeView {
    scheme: {
        id: string;
        name: string;
        archived?: boolean;
        description?: string;
    };
    steps: {
        step: DotCountWorkflowStep;
        actions: DotCountWorkflowAction[];
    }[];
}

/**
 * Response entity of `POST /api/v1/workflow/contentlet/actions/bulk`.
 *
 * Actions are deduped per scheme server-side and filtered to those flagged `showOn: LISTING`.
 * Archived schemes are excluded.
 */
export interface DotBulkActionView {
    schemes: DotBulkWorkflowSchemeView[];
}

/**
 * UI-facing shape: one scheme with its steps flattened into a single action list.
 *
 * This is what the Action Center renders — the nested step grouping is a backend implementation
 * detail that the dialog does not surface.
 */
export interface DotActionCenterScheme {
    id: string;
    name: string;
    /** Number of selected contentlets that sit somewhere in this scheme. */
    count: number;
    actions: DotActionCenterWorkflowAction[];
}

/**
 * A single selectable workflow action in the Action Center.
 */
export interface DotActionCenterWorkflowAction {
    id: string;
    name: string;
    count: number;
    /**
     * True when the action cannot be fired from the dialog without collecting extra input
     * (assign/comment, push-publish settings, or a move target path). Disabled in v1.
     */
    requiresInput: boolean;
    /** True when `count` is an upper bound because a Velocity condition was not evaluated. */
    approximateCount: boolean;
}
