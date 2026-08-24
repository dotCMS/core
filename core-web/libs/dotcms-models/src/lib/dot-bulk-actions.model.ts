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
 * Runtime inputs a workflow action needs before it can fire.
 *
 * These are the four — and only four — input kinds the backend advertises. `WorkflowResource`'s
 * `createActionInputViews()` is the sole producer of the `actionInputs[]` contract and emits exactly
 * these ids, so the set is closed by the API rather than by convention: an actionlet cannot ask for
 * anything else at fire time (every other actionlet takes its parameters at scheme-design time).
 *
 * Kept as separate flags rather than folded into one boolean because they are independent — a single
 * approval action can be assignable *and* commentable *and* push-publish — and because the dialog can
 * collect some of them but not yet all.
 */
export interface DotActionCenterActionInputs {
    /** Has a move actionlet with no configured path — needs a target path. */
    moveable: boolean;
    /** Has a push-publish actionlet — needs environments, dates and a filter. */
    pushPublish: boolean;
    /** Needs an assignee (user or role). */
    assignable: boolean;
    /** Needs a workflow comment. */
    commentable: boolean;
}

/**
 * A single selectable workflow action in the Action Center.
 */
export interface DotActionCenterWorkflowAction {
    id: string;
    name: string;
    count: number;
    /** Which runtime inputs the action needs before it can fire. */
    inputs: DotActionCenterActionInputs;
    /**
     * The action's default assignee role, which scopes the roles an assignable action may be given to.
     *
     * Carried from the underlying `DotCMSWorkflowAction` because the assignee picker needs it to ask
     * the backend for the right role list; it is meaningless when `inputs.assignable` is false.
     */
    nextAssign: string;
    /** Whether the assignable role list should follow the role hierarchy. Pairs with `nextAssign`. */
    roleHierarchyForAssign: boolean;
    /** True when `count` is an upper bound because a Velocity condition was not evaluated. */
    approximateCount: boolean;
    /**
     * Content type variable names whose contentlets can run this action.
     *
     * The bulk endpoint reports counts but never says *which* contentlets an action matches, so this
     * is reconstructed client-side by asking it once per content type in the selection. Workflow
     * schemes are assigned per content type, which makes the mapping exact at scheme level — enough
     * to keep a Blog action from listing a VtlInclude.
     *
     * Empty means "not resolved" (a single ungrouped lookup), and callers should fall back to the
     * whole selection rather than filtering everything out.
     */
    contentTypes: string[];
}
