import {
    DotActionCenterScheme,
    DotActionCenterWorkflowAction,
    DotBulkActionView,
    DotCMSContentlet,
    DotContentDriveItem,
    DotCountWorkflowAction
} from '@dotcms/dotcms-models';

import { isFolder } from './functions';
import { DEFAULT_WORKFLOW_ACTIONS, WORKFLOW_ACTION_ID } from './workflow-actions';

/**
 * A quick action as rendered in the Action Center: the action itself, an icon, and the number of
 * selected contentlets it applies to.
 */
export interface DotActionCenterQuickAction {
    id: WORKFLOW_ACTION_ID;
    /** i18n key for the label. */
    name: string;
    icon: string;
    count: number;
    /** Destructive action — rendered with the danger severity, as in the design. */
    danger: boolean;
}

/**
 * Quick actions offered in the Action Center, in display order.
 *
 * Scope notes for v1:
 * - Every entry here is a `SystemAction` the multi-contentlet endpoint accepts
 *   (`POST /api/v1/workflow/actions/default/fire/{systemAction}`), so each fires in one request.
 * - **Lock / Unlock are deliberately absent**: there is no bulk REST endpoint for them. The legacy
 *   JSP drives them through a Struts command (`full_unlock_list`) that loops server-side. Adding
 *   them means either a client-side N-call loop or a new `_bulklock` / `_bulkunlock` endpoint.
 * - **Add to Bundle is absent** for the same reason: it is only reachable through the legacy
 *   `RemotePublishAjaxAction` servlet, not a REST endpoint.
 *
 * `eligibleWhen` derives the count from row state the grid already has. It is a state heuristic,
 * not a permission check — an item can be counted and still fail at execution.
 */
const QUICK_ACTIONS: {
    id: WORKFLOW_ACTION_ID;
    icon: string;
    danger: boolean;
    eligibleWhen: (item: DotCMSContentlet) => boolean;
}[] = [
    {
        id: WORKFLOW_ACTION_ID.PUBLISH,
        icon: 'pi pi-upload',
        danger: false,
        eligibleWhen: (item) => !item.live && !item.archived
    },
    {
        id: WORKFLOW_ACTION_ID.UNPUBLISH,
        icon: 'pi pi-eye-slash',
        danger: false,
        eligibleWhen: (item) => !!item.live && !item.archived
    },
    {
        id: WORKFLOW_ACTION_ID.ARCHIVE,
        icon: 'pi pi-inbox',
        danger: true,
        eligibleWhen: (item) => !item.archived
    },
    {
        id: WORKFLOW_ACTION_ID.UNARCHIVE,
        icon: 'pi pi-undo',
        danger: false,
        eligibleWhen: (item) => !!item.archived
    },
    {
        id: WORKFLOW_ACTION_ID.DELETE,
        icon: 'pi pi-trash',
        danger: true,
        eligibleWhen: (item) => !!item.archived
    }
];

/**
 * Strips folders out of a selection. Folders can be selected in the grid but are ignored by every
 * bulk action: the endpoints take contentlet inodes, and folders carry no workflow step.
 *
 * @param items - The raw selection from the grid
 * @returns Only the contentlet items
 */
export const excludeFolders = (items: DotContentDriveItem[]): DotCMSContentlet[] =>
    items.filter((item): item is DotCMSContentlet => !isFolder(item));

/**
 * Maps a selection to the inode list expected by the bulk endpoints.
 *
 * @param items - The raw selection from the grid (folders are dropped)
 * @returns Contentlet inodes
 */
export const toContentletInodes = (items: DotContentDriveItem[]): string[] =>
    excludeFolders(items).map((item) => item.inode);

/**
 * Builds the Quick Actions list for the current selection, each with its eligible count.
 *
 * Actions that apply to nothing in the selection are dropped rather than shown with `(0)`, so the
 * list stays honest about what will actually happen.
 *
 * @param items - The raw selection from the grid
 * @returns Quick actions with a non-zero count, in display order
 */
export const getQuickActions = (items: DotContentDriveItem[]): DotActionCenterQuickAction[] => {
    const contentlets = excludeFolders(items);

    if (!contentlets.length) {
        return [];
    }

    return QUICK_ACTIONS.reduce<DotActionCenterQuickAction[]>((acc, quickAction) => {
        const count = contentlets.filter(quickAction.eligibleWhen).length;

        if (count === 0) {
            return acc;
        }

        const definition = DEFAULT_WORKFLOW_ACTIONS.find((action) => action.id === quickAction.id);

        acc.push({
            id: quickAction.id,
            name: definition?.name ?? quickAction.id,
            icon: quickAction.icon,
            danger: quickAction.danger,
            count
        });

        return acc;
    }, []);
};

/**
 * Flattens the `scheme -> steps -> actions` response into a single action list per scheme.
 *
 * The nested step grouping is a backend implementation detail the dialog does not surface. Two
 * things make the flattening safe:
 *
 * - The backend already sums an action's `count` across every step of its scheme, so concatenating
 *   step action lists does not inflate counts.
 * - The backend already dedupes actions per scheme, keeping a single occurrence.
 *
 * A defensive dedupe by action id is still applied (keeping the highest count) so a partially
 * out-of-sync index cannot render the same action twice.
 *
 * @param view - The raw `BulkActionView` response
 * @returns One entry per scheme, each with a flat action list; schemes with no actions are dropped
 */
export const toActionCenterSchemes = (view: DotBulkActionView): DotActionCenterScheme[] => {
    if (!view?.schemes?.length) {
        return [];
    }

    return view.schemes
        .map((schemeView) => {
            const byId = new Map<string, DotActionCenterWorkflowAction>();

            for (const { actions } of schemeView.steps ?? []) {
                for (const countAction of actions ?? []) {
                    const mapped = toActionCenterAction(countAction);
                    const existing = byId.get(mapped.id);

                    if (!existing || mapped.count > existing.count) {
                        byId.set(mapped.id, mapped);
                    }
                }
            }

            // Each contentlet sits in exactly one step, so summing step counts gives the number of
            // selected contentlets on this scheme.
            const count = (schemeView.steps ?? []).reduce(
                (total, { step }) => total + (step?.count ?? 0),
                0
            );

            return {
                id: schemeView.scheme.id,
                name: schemeView.scheme.name,
                count,
                actions: [...byId.values()].sort((a, b) => a.name.localeCompare(b.name))
            };
        })
        .filter((scheme) => scheme.actions.length > 0);
};

/**
 * Maps one `CountWorkflowAction` onto the UI shape.
 *
 * `requiresInput` folds together every flag that means the action cannot be fired straight from the
 * dialog: push-publish settings, a move target path, or an assign/comment prompt. v1 disables those
 * rather than reimplementing the params dialog.
 */
const toActionCenterAction = (
    countAction: DotCountWorkflowAction
): DotActionCenterWorkflowAction => {
    const { workflowAction, pushPublish, moveable, conditionPresent, count } = countAction;

    return {
        id: workflowAction.id,
        name: workflowAction.name,
        count,
        requiresInput:
            pushPublish || moveable || workflowAction.assignable || workflowAction.commentable,
        approximateCount: conditionPresent
    };
};
