import {
    DotActionCenterScheme,
    DotActionCenterWorkflowAction,
    DotBulkActionView,
    DotCMSContentlet,
    DotContentDriveItem,
    DotCountWorkflowAction
} from '@dotcms/dotcms-models';

import { isFolder } from './functions';
import { WORKFLOW_ACTION_ID } from './workflow-actions';

/** Quick action that is not a `SystemAction` and so is not fired through the workflow endpoints. */
export const ADD_TO_BUNDLE_ACTION_ID = 'ADD_TO_BUNDLE';

export type DotActionCenterQuickActionId = WORKFLOW_ACTION_ID | typeof ADD_TO_BUNDLE_ACTION_ID;

/**
 * A quick action as rendered in the Action Center: the action itself, an icon, and the number of
 * selected contentlets it applies to.
 */
export interface DotActionCenterQuickAction {
    id: DotActionCenterQuickActionId;
    /** i18n key for the label. */
    name: string;
    /** Material Symbols glyph name, rendered inside the row's icon chip. */
    icon: string;
    /**
     * The contentlet inodes the action applies to — exactly the set that gets fired.
     *
     * Derived alongside {@link count} from one filter pass so the number shown on the row and the
     * items actually acted on cannot drift apart.
     */
    eligibleInodes: string[];
    /**
     * Number of selected contentlets the action applies to. `0` means it does not apply to this
     * selection at all; the row is still rendered, but not selectable.
     */
    count: number;
    /** Destructive action — rendered with the danger severity, as in the design. */
    danger: boolean;
    /** i18n key for a confirmation prompt shown before firing, when the action warrants one. */
    confirmMessage?: string;
    /**
     * i18n key explaining why the action cannot be run at all yet, independent of the selection.
     * When set the row is always non-selectable and shows this as its hint.
     */
    pendingHint?: string;
    /**
     * How many of the {@link eligibleInodes} are expected to fail, with {@link warningHint}
     * explaining why. `0` for actions with nothing to warn about.
     *
     * These items are still fired — the count is a heads-up, not a filter. See the Unlock entry in
     * {@link QUICK_ACTIONS} for why the client cannot decide this on its own.
     */
    warningCount: number;
    /** i18n key describing what {@link warningCount} counts. Absent when it is `0`. */
    warningHint?: string;
}

/**
 * Quick actions offered in the Action Center.
 *
 * **The order of this array is the display order and is fixed** — Lock, Unlock, Publish, Unpublish,
 * Archive, Delete, Unarchive, Add to Bundle. Rows keep their position whether or not they are
 * selectable, so the list never reshuffles as the selection changes.
 *
 * Scope notes for v1:
 * - Every entry except Add to Bundle is a `SystemAction` the multi-contentlet endpoint accepts
 *   (`POST /api/v1/workflow/actions/default/fire/{systemAction}`), so each fires in one request.
 * - **Add to Bundle is present but always disabled.** `POST /api/v1/bundles/assets` does accept a
 *   list of asset identifiers, so the endpoint is not the blocker: it needs a target bundle, which
 *   means a picker step (`DotAddToBundleComponent` takes a single identifier today) and an
 *   enterprise-license gate. Tracked separately.
 *
 * `eligibleWhen` derives the count from row state the grid already has. It is a state heuristic,
 * not a permission check — an item can be counted and still fail at execution.
 *
 * `warnWhen` marks eligible items that are *likely* to fail, so the row can say so up front without
 * dropping them from the payload.
 */
const QUICK_ACTIONS: {
    id: DotActionCenterQuickActionId;
    /** Label key. Held here rather than borrowed from the toolbar's action list, which no longer
     *  carries these actions now that the Workflow Center owns them. */
    nameKey: string;
    icon: string;
    danger: boolean;
    eligibleWhen: (item: DotCMSContentlet) => boolean;
    /** Confirmation message key. Set for actions destructive enough to warrant a prompt. */
    confirmMessage?: string;
    pendingHint?: string;
    /** Counted among the eligible items to produce `warningCount`. */
    warnWhen?: (item: DotCMSContentlet) => boolean;
    /** Explains what `warnWhen` matched. Required whenever `warnWhen` is set. */
    warningHint?: string;
}[] = [
    {
        // Lock and Unlock lead the list: they are the least destructive actions here and the ones a
        // user reaches for mid-edit, so they sit furthest from Archive and Delete.
        id: WORKFLOW_ACTION_ID.LOCK,
        nameKey: 'content-drive.context-menu.lock',
        icon: 'lock',
        danger: false,
        // Archived content is a dead end until unarchived, and `deleteContentlets` honours
        // `canLock`, so locking an archived item would quietly make it undeletable by anyone but
        // the lock holder.
        eligibleWhen: (item) => !item.locked && !item.archived
    },
    {
        id: WORKFLOW_ACTION_ID.UNLOCK,
        nameKey: 'content-drive.context-menu.unlock',
        icon: 'lock_open',
        danger: false,
        // `locked` alone would do — archive refuses locked content — but the archived exclusion is
        // spelled out so the pair reads the same way.
        eligibleWhen: (item) => !!item.locked && !item.archived,
        // A lock belonging to someone else can only be released by a CMS Administrator, and the
        // grid has no idea whether the current user holds that role. So these items are counted,
        // fired, and reported on rather than filtered out: `contentEditable` is false on a locked
        // row the current user does not hold.
        warnWhen: (item) => !item.contentEditable,
        warningHint: 'content-drive.action-center.unlock.locked-by-others'
    },
    {
        id: WORKFLOW_ACTION_ID.PUBLISH,
        nameKey: 'Default-Action-Publish',
        icon: 'publish',
        danger: false,
        eligibleWhen: (item) => !item.live && !item.archived
    },
    {
        id: WORKFLOW_ACTION_ID.UNPUBLISH,
        nameKey: 'Default-Action-Unpublish',
        icon: 'visibility_off',
        danger: false,
        eligibleWhen: (item) => !!item.live && !item.archived
    },
    {
        id: WORKFLOW_ACTION_ID.ARCHIVE,
        nameKey: 'Default-Action-Archive',
        icon: 'archive',
        danger: true,
        eligibleWhen: (item) => !item.archived
    },
    {
        id: WORKFLOW_ACTION_ID.DELETE,
        nameKey: 'Default-Action-Delete',
        icon: 'delete',
        danger: true,
        eligibleWhen: (item) => !!item.archived,
        // Carried over from the toolbar's Delete, which prompted before firing. Without this the
        // move into the Workflow Center would have quietly dropped the only guard on a bulk delete.
        confirmMessage: 'content.drive.worflow.action.delete.confirm'
    },
    {
        // Sits after Delete so the two archived-only actions are adjacent, and so archiving is not
        // a one-way trip: without it nothing in this dialog can un-archive.
        id: WORKFLOW_ACTION_ID.UNARCHIVE,
        nameKey: 'Default-Action-Unarchive',
        icon: 'unarchive',
        danger: false,
        eligibleWhen: (item) => !!item.archived
    },
    {
        id: ADD_TO_BUNDLE_ACTION_ID,
        nameKey: 'content-drive.action-center.add-to-bundle',
        icon: 'inventory_2',
        danger: false,
        // A bundle accepts any asset, so the count is the whole contentlet selection. It is shown
        // for honesty about what the action would cover once the picker exists.
        eligibleWhen: () => true,
        pendingHint: 'content-drive.action-center.add-to-bundle.pending'
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
 * Every action is always returned, including those that apply to nothing — a `count` of `0` means
 * "does not apply to this selection", and the dialog renders those rows as non-selectable. Keeping
 * them visible makes the set of available actions stable as the selection changes, instead of rows
 * appearing and disappearing under the pointer.
 *
 * An empty result means there are no contentlets at all (an empty or folder-only selection), where
 * no action could apply.
 *
 * @param items - The raw selection from the grid
 * @returns Every quick action in display order, each with its eligible count (possibly `0`)
 */
export const getQuickActions = (items: DotContentDriveItem[]): DotActionCenterQuickAction[] => {
    const contentlets = excludeFolders(items);

    if (!contentlets.length) {
        return [];
    }

    return QUICK_ACTIONS.map((quickAction) => {
        // One filter pass feeds both the count and the inodes that get fired, so the row can never
        // advertise a different number of items than the action actually touches.
        const eligible = contentlets.filter(quickAction.eligibleWhen);
        const eligibleInodes = eligible.map((item) => item.inode);
        // Counted over `eligible` rather than the whole selection: warning about items the action
        // was never going to touch would make the number meaningless.
        const warningCount = quickAction.warnWhen
            ? eligible.filter(quickAction.warnWhen).length
            : 0;

        return {
            id: quickAction.id,
            name: quickAction.nameKey,
            icon: quickAction.icon,
            danger: quickAction.danger,
            eligibleInodes,
            count: eligibleInodes.length,
            confirmMessage: quickAction.confirmMessage,
            pendingHint: quickAction.pendingHint,
            warningCount,
            warningHint: warningCount > 0 ? quickAction.warningHint : undefined
        };
    });
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
 * Groups a contentlet selection by content type variable name, preserving selection order.
 *
 * Used to split the bulk-actions lookup into one request per content type, which is what makes
 * per-action eligibility knowable at all — see {@link mergeActionCenterSchemes}.
 *
 * @param contentlets - Contentlets (folders already excluded)
 * @returns One entry per distinct content type
 */
export const groupByContentType = (
    contentlets: DotCMSContentlet[]
): { contentType: string; contentlets: DotCMSContentlet[] }[] => {
    const groups = new Map<string, DotCMSContentlet[]>();

    for (const contentlet of contentlets) {
        const existing = groups.get(contentlet.contentType);

        if (existing) {
            existing.push(contentlet);
        } else {
            groups.set(contentlet.contentType, [contentlet]);
        }
    }

    return [...groups].map(([contentType, items]) => ({ contentType, contentlets: items }));
};

/**
 * Merges per-content-type bulk-action responses into one scheme list, recording which content types
 * contributed each action.
 *
 * The endpoint only ever reports counts, so a single lookup over a mixed selection cannot say which
 * contentlets an action applies to. Asking once per content type recovers that: schemes are assigned
 * per content type, so an action returned for the "Blog" request applies to Blog contentlets and no
 * others.
 *
 * Counts are summed across groups, which reproduces exactly what a single combined lookup would have
 * reported — each contentlet is counted by exactly one group.
 *
 * @param groups - One `(contentType, response)` pair per content type in the selection
 * @returns Merged schemes, each action carrying its eligible content types
 */
export const mergeActionCenterSchemes = (
    groups: { contentType: string; view: DotBulkActionView }[]
): DotActionCenterScheme[] => {
    const bySchemeId = new Map<string, DotActionCenterScheme>();

    for (const { contentType, view } of groups) {
        for (const scheme of toActionCenterSchemes(view)) {
            const existing = bySchemeId.get(scheme.id);

            if (!existing) {
                bySchemeId.set(scheme.id, {
                    ...scheme,
                    actions: scheme.actions.map((action) => ({
                        ...action,
                        contentTypes: [contentType]
                    }))
                });

                continue;
            }

            existing.count += scheme.count;

            for (const action of scheme.actions) {
                const merged = existing.actions.find((candidate) => candidate.id === action.id);

                if (!merged) {
                    existing.actions.push({ ...action, contentTypes: [contentType] });

                    continue;
                }

                merged.count += action.count;
                // Approximate wins: if the condition was unevaluated for any group, the total is an
                // upper bound too.
                merged.approximateCount = merged.approximateCount || action.approximateCount;

                if (!merged.contentTypes.includes(contentType)) {
                    merged.contentTypes.push(contentType);
                }
            }

            existing.actions.sort((a, b) => a.name.localeCompare(b.name));
        }
    }

    return [...bySchemeId.values()];
};

/**
 * Narrows a selection to the contentlets a workflow action can actually run on.
 *
 * Falls back to the whole selection when the action has no resolved content types, so an unresolved
 * lookup shows too many rows rather than none.
 *
 * Scheme-level accurate, not step-level: two contentlets of the same content type can sit on
 * different steps, and only one of those steps may expose the action. The action's own `count` stays
 * the authority on the true total, which is why the dialog still warns when it falls short.
 *
 * @param action - The selected workflow action
 * @param contentlets - Contentlets in the selection (folders already excluded)
 * @returns The contentlets whose content type exposes the action
 */
export const eligibleContentlets = (
    action: DotActionCenterWorkflowAction | undefined,
    contentlets: DotCMSContentlet[]
): DotCMSContentlet[] => {
    if (!action?.contentTypes.length) {
        return contentlets;
    }

    return contentlets.filter((item) => action.contentTypes.includes(item.contentType));
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
        approximateCount: conditionPresent,
        // Stamped by `mergeActionCenterSchemes`, which is the only caller that knows which content
        // type a response came from.
        contentTypes: []
    };
};
