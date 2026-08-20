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

/**
 * Action Center helpers.
 *
 * Two action sources, one dialog:
 * 1. **Quick actions** — fixed list (`QUICK_ACTIONS`), eligibility from row state.
 * 2. **Workflow actions** — from bulk-actions API, one request per content type, then merged.
 *
 * Folders are always dropped: endpoints take contentlet inodes only.
 */

/** Not a `SystemAction`; not fired through workflow endpoints. */
export const ADD_TO_BUNDLE_ACTION_ID = 'ADD_TO_BUNDLE';

/**
 * Push Publish over the selection — the old search toolbar's own bulk action, not a `SystemAction`.
 *
 * Collects environments, a schedule and a filter on the configuration step (the same
 * `DotWorkflowPushPublishComponent` the workflow-action path uses), then posts the whole selection to
 * `RemotePublishAjaxAction` as comma-joined identifiers.
 *
 * Unavailable when the instance has no push publish environment the current user's role can send to —
 * see {@link DotActionCenterContext.hasPushPublishEnvironments}.
 */
export const PUSH_PUBLISH_ACTION_ID = 'PUSH_PUBLISH';

/**
 * Reindex the selection — the old search toolbar's "Refresh", backed by `_bulkrefresh`.
 *
 * Placeholder: rendered but not wired. The endpoint streams progress over SSE and is not job-backed,
 * so it cannot reuse the synchronous `bulkFire` path the other quick actions run on.
 */
export const REFRESH_ACTION_ID = 'REFRESH';

export type DotActionCenterQuickActionId =
    | WORKFLOW_ACTION_ID
    | typeof ADD_TO_BUNDLE_ACTION_ID
    | typeof PUSH_PUBLISH_ACTION_ID
    | typeof REFRESH_ACTION_ID;

/** Quick action as rendered in the dialog (with eligibility counts). */
export interface DotActionCenterQuickAction {
    id: DotActionCenterQuickActionId;
    /** i18n label key. */
    name: string;
    /** Material Symbols glyph name. */
    icon: string;
    /**
     * The ids the action will fire on. Built with {@link count} in one pass so the badge and the
     * payload cannot diverge.
     *
     * **Inodes for the contentlet-only actions**, which is what pins the version and therefore the
     * step a fire lands on, so one contentlet sitting on two steps contributes two entries.
     * **Identifiers for the folder-capable ones** (Add to Bundle, Push Publish), which send the
     * asset rather than a version and accept a folder, and a folder has no inode. Same asymmetry
     * `executeAddToBundle` already documents on the store side.
     */
    eligibleInodes: string[];
    /** Eligible contentlets. `0` = shown but not selectable. */
    count: number;
    /**
     * Eligible items likely to fail — heads-up only; they are still fired.
     * See Unlock in {@link QUICK_ACTIONS}.
     */
    warningCount: number;
    /** i18n key for {@link warningCount}. Absent when count is `0`. */
    warningHint?: string;
    /**
     * Shown, disabled, and not yet wired to anything.
     *
     * Deliberately rendered rather than hidden: these are actions the old search toolbar offers, and
     * leaving them out of the list makes Content Drive look like it dropped them instead of not having
     * reached them yet. Disabled with a tooltip is the honest state.
     */
    comingSoon: boolean;
    /**
     * Wired, but unusable here because the instance has no push publish environment configured for
     * this user's role.
     *
     * Distinct from {@link comingSoon}: nothing is missing from dotCMS, something is missing from the
     * *configuration*, and the fix is an administrator's rather than ours. The row says which.
     */
    missingEnvironments: boolean;
}

/** Caller state predicates need beyond row data. */
export interface DotActionCenterContext {
    /**
     * CMS Administrator role. `false` while unresolved → treated as non-admin
     * (see {@link isLockedByAnotherUser}).
     */
    isAdmin: boolean;
    /**
     * At least one push publish environment is reachable by this user's role.
     *
     * `undefined` means "not looked up yet" and reads the same as "none": Push Publish stays disabled
     * until the answer arrives, rather than enabling for a moment and then retracting.
     */
    hasPushPublishEnvironments?: boolean;
}

/**
 * Definition of a quick action before selection counts are applied.
 * {@link getQuickActions} maps these into {@link DotActionCenterQuickAction}.
 */
interface DotActionCenterQuickActionDef {
    id: DotActionCenterQuickActionId;
    /**
     * i18n label key. Owned here (not shared with the toolbar) now that the
     * Action Center owns these actions.
     */
    nameKey: string;
    icon: string;
    /** Row-state heuristic — not a permission check. Counted items can still fail at fire. */
    eligibleWhen: (item: DotCMSContentlet) => boolean;
    /** Among eligible items; feeds `warningCount`. */
    warnWhen?: (item: DotCMSContentlet, context: DotActionCenterContext) => boolean;
    /** Required whenever `warnWhen` is set. */
    warningHint?: string;
    /** Rendered but disabled — see {@link DotActionCenterQuickAction.comingSoon}. */
    comingSoon?: boolean;
    /** Needs at least one push publish environment before it can run. */
    requiresEnvironments?: boolean;
    /**
     * Runs on folders as well as contentlets.
     *
     * Only for actions that send the *asset* by identifier and whose `eligibleWhen` ignores row
     * state, since a folder has none of it. The bulk endpoints behind everything else take
     * contentlet inodes, and folders have no workflow, so they are contentlet-only by nature
     * rather than by omission.
     */
    supportsFolders?: boolean;
}

/**
 * True when the row is locked by someone else *and* that matters for the current user.
 *
 * Shared by the Unlock warning badge and the preview markers so those counts stay in sync.
 *
 * - Grid `contentEditable` ≈ lock owner (`lockedBy == currentUser` + WRITE), not role.
 * - Server `canLock` lets CMS Admins unlock anyone — so admins get no warning.
 * - Known over-warn: EDIT on content type (not contentlet) still fails `contentEditable`
 *   while `canLock` allows it. Hint says "may require" for that reason.
 */
export const isLockedByAnotherUser = (
    item: DotCMSContentlet,
    { isAdmin }: DotActionCenterContext
): boolean => !isAdmin && !!item.locked && !item.contentEditable;

/**
 * Quick actions in display order (fixed — rows never reshuffle).
 *
 * Order: Lock, Unlock, Add to Bundle, Push Publish, Refresh.
 *
 * **Scope: the old search toolbar's bulk operations, and only those.** Publish, Unpublish, Archive,
 * Unarchive and Delete used to sit here as well, fired through
 * `POST .../workflow/actions/default/fire/{systemAction}`. They were removed because the Workflow
 * Actions section below already offers them — as the *scheme's own* actions, which is the accurate
 * answer for a contentlet whose scheme maps `PUBLISH` to something other than a plain publish. Two
 * rows labelled "Publish" that resolve differently is worse than one that resolves correctly.
 *
 * What is left is what the old search offered outside the workflow dropdown: Lock and Unlock (per-user
 * state, no workflow transition, so they have no scheme action to defer to), Add to Bundle, Push
 * Publish and Refresh.
 *
 * Lock/Unlock fire through the system-action endpoint; Add to Bundle posts to the legacy bundle
 * servlet and collects a target first. Push Publish and Refresh are placeholders — see
 * {@link DotActionCenterQuickAction.comingSoon}.
 */
const QUICK_ACTIONS: DotActionCenterQuickActionDef[] = [
    {
        id: WORKFLOW_ACTION_ID.LOCK,
        nameKey: 'content-drive.context-menu.lock',
        icon: 'lock',
        // UX filter only: locking archived content has no upside and can block delete
        // (`canLock` is a delete precondition). Server still allows it.
        eligibleWhen: (item) => !item.locked && !item.archived
    },
    {
        id: WORKFLOW_ACTION_ID.UNLOCK,
        nameKey: 'content-drive.context-menu.unlock',
        icon: 'lock_open',
        eligibleWhen: (item) => !!item.locked && !item.archived,
        // Warn, don't filter — only the server knows if unlock will succeed.
        warnWhen: isLockedByAnotherUser,
        warningHint: 'content-drive.action-center.unlock.locked-by-others'
    },
    {
        id: ADD_TO_BUNDLE_ACTION_ID,
        nameKey: 'content-drive.action-center.add-to-bundle',
        icon: 'inventory_2',
        // Every contentlet can go in a bundle: no row state disqualifies one. Coverage is the whole
        // selection, minus the identifier collapse the configuration step explains.
        eligibleWhen: () => true,
        supportsFolders: true
    },
    {
        id: PUSH_PUBLISH_ACTION_ID,
        nameKey: 'Remote-Publish',
        icon: 'cloud_upload',
        // No contentlet state disqualifies a push; the environment does, and that is not a per-row
        // question. Counted over the whole selection so the row reports what it would send.
        eligibleWhen: () => true,
        requiresEnvironments: true,
        supportsFolders: true
    },
    {
        id: REFRESH_ACTION_ID,
        nameKey: 'Refresh',
        icon: 'refresh',
        eligibleWhen: () => true,
        comingSoon: true
    }
];

/**
 * Whether a quick action runs on folders as well as contentlets.
 *
 * Read from the same registry the rows are built from, so the dialog cannot disagree with
 * {@link getQuickActions} about which key an action's `eligibleInodes` holds.
 */
export const supportsFolders = (id: DotActionCenterQuickActionId): boolean =>
    !!QUICK_ACTIONS.find((quickAction) => quickAction.id === id)?.supportsFolders;

/** Drops folders from a selection (bulk endpoints are contentlet-only). */
export const excludeFolders = (items: DotContentDriveItem[]): DotCMSContentlet[] =>
    items.filter((item): item is DotCMSContentlet => !isFolder(item));

/**
 * Distinct identifiers among the given contentlets, in first-seen order.
 *
 * Bundles hold one entry per identifier, so every language version of a contentlet is the same asset.
 * The endpoint dedupes server-side either way ("Multiples languages have the same identifier"); doing
 * it here as well is what lets the dialog say how many assets it is really about to add, instead of
 * promising a row count the result will silently undercut.
 */
export const toDistinctIdentifiers = (contentlets: DotCMSContentlet[]): string[] => [
    ...new Set(contentlets.map((item) => item.identifier).filter(Boolean))
];

/** Contentlet inodes for bulk endpoints (folders dropped). */
export const toContentletInodes = (items: DotContentDriveItem[]): string[] =>
    excludeFolders(items).map((item) => item.inode);

/**
 * Quick actions for the current selection, always in {@link QUICK_ACTIONS} order.
 *
 * Every action is returned — `count: 0` means not applicable but still shown, so the
 * list stays stable as the selection changes. Empty array only when there are no contentlets.
 *
 * @param context Defaults to non-admin (safe over-warning).
 */
export const getQuickActions = (
    items: DotContentDriveItem[],
    context: DotActionCenterContext = { isAdmin: false }
): DotActionCenterQuickAction[] => {
    if (!items.length) {
        return [];
    }

    const contentlets = excludeFolders(items);

    return QUICK_ACTIONS.flatMap((quickAction) => {
        // Folder-capable actions see the whole selection; everything else sees contentlets only.
        const scoped = quickAction.supportsFolders ? items : contentlets;

        // Dropped rather than shown with a count of `0`: a folder-only selection is not "no eligible
        // rows", it is an action that does not apply to what is selected, and a disabled Lock row
        // over a folder selection is noise rather than information.
        if (!scoped.length) {
            return [];
        }

        // Safe because a folder-capable action's `eligibleWhen` ignores the row — that is the
        // precondition for setting `supportsFolders`, since a folder carries none of the state the
        // contentlet-only predicates read.
        const eligible = (scoped as DotCMSContentlet[]).filter(quickAction.eligibleWhen);
        // One pass → count and fired ids stay aligned. See `eligibleInodes` for why the key differs
        // by action.
        const eligibleInodes = eligible.map((item) =>
            quickAction.supportsFolders ? item.identifier : item.inode
        );
        const { warnWhen } = quickAction;
        const warningCount = warnWhen
            ? eligible.filter((item) => warnWhen(item, context)).length
            : 0;

        return {
            id: quickAction.id,
            name: quickAction.nameKey,
            icon: quickAction.icon,
            eligibleInodes,
            count: eligibleInodes.length,
            warningCount,
            warningHint: warningCount > 0 ? quickAction.warningHint : undefined,
            comingSoon: !!quickAction.comingSoon,
            missingEnvironments:
                !!quickAction.requiresEnvironments && !context.hasPushPublishEnvironments
        };
    });
};

/**
 * Flattens `scheme → steps → actions` into one action list per scheme.
 *
 * Backend already sums counts and dedupes per scheme; we still dedupe by id
 * (keep highest count) as a safety net. Schemes with no actions are dropped.
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

            // One contentlet per step → sum step counts = scheme coverage.
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
 * Groups contentlets by content type, preserving selection order.
 * Enables per-type bulk lookups — see {@link mergeActionCenterSchemes}.
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
 * Merges per-content-type bulk responses into one scheme list.
 *
 * A single mixed lookup only returns counts — not which contentlets match.
 * One request per content type recovers that; each action gets `contentTypes`.
 * Counts sum to the same totals a combined lookup would report.
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
                // Unevaluated condition in any group → total is an upper bound.
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
 * Contentlets a workflow action can run on (by content type).
 *
 * Falls back to the full selection when `contentTypes` is empty (prefer over-include).
 * Scheme-level only — same type, different steps, may still diverge from `action.count`.
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
 * A configuration section the dialog knows how to render.
 *
 * `assignComment` is **one** section covering two flags: `DotWorkflowAssignCommentComponent` renders an
 * assignee, a comment, or both, so an action declaring both still needs a single section.
 */
export type DotActionInputKind = 'move' | 'assignComment' | 'pushPublish';

/**
 * The configuration sections an action needs, in the order they are shown.
 *
 * Every section is rendered together on one screen, so this is a render order rather than a page
 * sequence. The order still matches the legacy wizard's — move and assign/comment before push publish —
 * so a user moving between the two dialogs meets the same arrangement.
 *
 * An empty result means the action fires straight from the selection, and there is no configuration
 * screen at all.
 */
export const requiredInputKinds = (
    action: DotActionCenterWorkflowAction | undefined
): DotActionInputKind[] => {
    if (!action) {
        return [];
    }

    const { moveable, assignable, commentable, pushPublish } = action.inputs;

    return [
        ...(moveable ? (['move'] as const) : []),
        ...(assignable || commentable ? (['assignComment'] as const) : []),
        ...(pushPublish ? (['pushPublish'] as const) : [])
    ];
};

/**
 * Converts the host/folder field's value into the `_path_to_move` the bulk endpoint expects.
 *
 * `DotHostFolderFieldComponent` writes `hostname:/folder/path` — the format the Site-or-Folder *content
 * field* persists. `MoveContentActionlet` reads `//hostname/folder/path` instead, the same shape Content
 * Drive's drag-and-drop move already builds. Neither side is wrong; they are different contracts that
 * happen to meet here, so the seam gets one named function instead of an inline template expression.
 *
 * Returns an empty string for an unset or unrecognised value, which callers treat as "no path chosen"
 * — never a silently malformed path that the server would reject with "The host path is not valid".
 */
export const toPathToMove = (hostFolderValue: string | null | undefined): string => {
    const separatorIndex = hostFolderValue?.indexOf(':') ?? -1;

    if (!hostFolderValue || separatorIndex < 1) {
        return '';
    }

    const hostname = hostFolderValue.slice(0, separatorIndex);
    const path = hostFolderValue.slice(separatorIndex + 1);

    return `//${hostname}${path.startsWith('/') ? path : `/${path}`}`;
};

/**
 * Inverse of {@link toPathToMove}: `//hostname/path` → the `hostname:/path` the picker reads.
 *
 * Used to seed the picker with the folder Content Drive is currently browsing, which is what makes it
 * open on the tree already expanded to that folder rather than at the site list. The picker has no
 * separate "start here" input — `writeValue` is the only channel that drives its initial load — so the
 * starting location has to arrive as a value.
 *
 * Returns an empty string for anything it cannot parse, which leaves the picker unseeded rather than
 * feeding it a path it would fail to resolve.
 */
export const toHostFolderValue = (pathToMove: string | null | undefined): string => {
    if (!pathToMove?.startsWith('//')) {
        return '';
    }

    const withoutPrefix = pathToMove.slice(2);
    const separatorIndex = withoutPrefix.indexOf('/');

    if (separatorIndex < 1) {
        // A bare `//hostname` is the site root; the picker still expects an explicit path.
        return withoutPrefix ? `${withoutPrefix}:/` : '';
    }

    return `${withoutPrefix.slice(0, separatorIndex)}:${withoutPrefix.slice(separatorIndex)}`;
};

/**
 * Maps API `CountWorkflowAction` → UI shape.
 *
 * The four input flags are carried through individually rather than collapsed: each maps to a section
 * the configuration screen renders, and a roll-up boolean would say nothing about which.
 */
const toActionCenterAction = (
    countAction: DotCountWorkflowAction
): DotActionCenterWorkflowAction => {
    const { workflowAction, pushPublish, moveable, conditionPresent, count } = countAction;

    const inputs = {
        moveable,
        pushPublish,
        assignable: workflowAction.assignable,
        commentable: workflowAction.commentable
    };

    return {
        id: workflowAction.id,
        name: workflowAction.name,
        count,
        inputs,
        // Only meaningful for an assignable action, but carried unconditionally so the assignee picker
        // does not need a second lookup to find the role list it should offer.
        nextAssign: workflowAction.nextAssign,
        roleHierarchyForAssign: workflowAction.roleHierarchyForAssign,
        approximateCount: conditionPresent,
        // Filled by `mergeActionCenterSchemes`.
        contentTypes: []
    };
};
