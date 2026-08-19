import { describe, expect, it } from '@jest/globals';

import {
    DotActionCenterWorkflowAction,
    DotBulkActionView,
    DotCMSContentlet,
    DotContentDriveItem
} from '@dotcms/dotcms-models';

import {
    ADD_TO_BUNDLE_ACTION_ID,
    eligibleContentlets,
    excludeFolders,
    getQuickActions,
    groupByContentType,
    isLockedByAnotherUser,
    mergeActionCenterSchemes,
    PUSH_PUBLISH_ACTION_ID,
    REFRESH_ACTION_ID,
    requiredInputKinds,
    toActionCenterSchemes,
    toContentletInodes,
    toHostFolderValue,
    toPathToMove
} from './action-center';
import { WORKFLOW_ACTION_ID } from './workflow-actions';

const contentlet = (
    overrides: Partial<DotContentDriveItem> & { inode: string }
): DotContentDriveItem =>
    ({
        baseType: 'CONTENT',
        live: false,
        working: true,
        archived: false,
        locked: false,
        ...overrides
    }) as DotContentDriveItem;

const folder = (inode: string): DotContentDriveItem =>
    ({ type: 'folder', inode, identifier: inode }) as unknown as DotContentDriveItem;

/** An action that fires straight from the selection. */
const NO_INPUTS = {
    moveable: false,
    pushPublish: false,
    assignable: false,
    commentable: false
};

/** Builds a bare action carrying only the input flags under test. */
const actionWithInputs = (
    inputs: Partial<DotActionCenterWorkflowAction['inputs']>
): DotActionCenterWorkflowAction =>
    ({
        id: 'a1',
        name: 'Action',
        count: 1,
        inputs: { ...NO_INPUTS, ...inputs },
        contentTypes: []
    }) as DotActionCenterWorkflowAction;

/**
 * Builds a `BulkActionView` fixture. `steps` is a list of `[stepCount, actions]` pairs so tests can
 * express the same action appearing on multiple steps.
 */
const bulkActionView = (
    schemeName: string,
    steps: [
        number,
        { id: string; name: string; count: number; flags?: Record<string, boolean> }[]
    ][]
): DotBulkActionView =>
    ({
        schemes: [
            {
                scheme: { id: `${schemeName}-id`, name: schemeName },
                steps: steps.map(([count, actions], index) => ({
                    step: {
                        count,
                        workflowStep: {
                            id: `step-${index}`,
                            name: `Step ${index}`,
                            schemeId: `${schemeName}-id`
                        }
                    },
                    actions: actions.map((action) => ({
                        count: action.count,
                        pushPublish: action.flags?.pushPublish ?? false,
                        moveable: action.flags?.moveable ?? false,
                        conditionPresent: action.flags?.conditionPresent ?? false,
                        workflowAction: {
                            id: action.id,
                            name: action.name,
                            assignable: action.flags?.assignable ?? false,
                            commentable: action.flags?.commentable ?? false
                        }
                    }))
                }))
            }
        ]
    }) as DotBulkActionView;

describe('action-center utils', () => {
    describe('excludeFolders', () => {
        it('should drop folders and keep contentlets', () => {
            const items = [contentlet({ inode: 'a' }), folder('f1'), contentlet({ inode: 'b' })];

            expect(excludeFolders(items).map((item) => item.inode)).toEqual(['a', 'b']);
        });

        it('should return an empty array when everything is a folder', () => {
            expect(excludeFolders([folder('f1'), folder('f2')])).toEqual([]);
        });
    });

    describe('toContentletInodes', () => {
        it('should map to inodes, folders excluded', () => {
            const items = [contentlet({ inode: 'a' }), folder('f1')];

            expect(toContentletInodes(items)).toEqual(['a']);
        });
    });

    describe('isLockedByAnotherUser', () => {
        const row = (overrides: Partial<DotCMSContentlet>) =>
            contentlet({ inode: 'a', ...overrides }) as DotCMSContentlet;

        it('should flag a locked row the current user cannot edit', () => {
            expect(
                isLockedByAnotherUser(row({ locked: true, contentEditable: false }), {
                    isAdmin: false
                })
            ).toBe(true);
        });

        it('should not flag a lock the current user holds', () => {
            expect(
                isLockedByAnotherUser(row({ locked: true, contentEditable: true }), {
                    isAdmin: false
                })
            ).toBe(false);
        });

        it('should not flag an unlocked row', () => {
            // `contentEditable` is false on plenty of unlocked rows (no WRITE permission); only a
            // lock makes it mean "somebody else holds this".
            expect(
                isLockedByAnotherUser(row({ locked: false, contentEditable: false }), {
                    isAdmin: false
                })
            ).toBe(false);
        });

        it('should never flag anything for an administrator', () => {
            expect(
                isLockedByAnotherUser(row({ locked: true, contentEditable: false }), {
                    isAdmin: true
                })
            ).toBe(false);
        });
    });

    describe('getQuickActions', () => {
        it('should return no actions for an empty selection', () => {
            expect(getQuickActions([])).toEqual([]);
        });

        it('should return no actions for a folder-only selection', () => {
            expect(getQuickActions([folder('f1')])).toEqual([]);
        });

        it('should not offer the workflow state actions as quick actions', () => {
            // Publish, Unpublish, Archive, Unarchive and Delete are the scheme's own actions and
            // are reached through the Workflow Actions section, where they resolve to whatever the
            // content type's scheme actually maps them to.
            const ids = getQuickActions([
                contentlet({ inode: 'a', archived: true, live: true, locked: true })
            ]).map((action) => action.id);

            expect(ids).not.toContain(WORKFLOW_ACTION_ID.PUBLISH);
            expect(ids).not.toContain(WORKFLOW_ACTION_ID.UNPUBLISH);
            expect(ids).not.toContain(WORKFLOW_ACTION_ID.ARCHIVE);
            expect(ids).not.toContain(WORKFLOW_ACTION_ID.UNARCHIVE);
            expect(ids).not.toContain(WORKFLOW_ACTION_ID.DELETE);
        });

        it('should count Lock for items that are not locked', () => {
            const items = [
                contentlet({ inode: 'a', locked: false }),
                contentlet({ inode: 'b', locked: true }),
                contentlet({ inode: 'c', locked: false })
            ];

            const lock = getQuickActions(items).find(
                (action) => action.id === WORKFLOW_ACTION_ID.LOCK
            );

            expect(lock?.count).toBe(2);
            expect(lock?.eligibleInodes).toEqual(['a', 'c']);
        });

        it('should not count archived items as lockable or unlockable', () => {
            // Locking an archived row serves no purpose and leaves it undeletable by anyone but
            // the holder, since `canLock` is a delete precondition. Unlock is covered by `locked`
            // alone (archive refuses locked content), but excluded explicitly so the pair reads the
            // same. This narrows the dialog only — the lock endpoints accept archived content.
            const items = [contentlet({ inode: 'a', archived: true, locked: false })];

            const byId = new Map(getQuickActions(items).map((action) => [action.id, action.count]));

            expect(byId.get(WORKFLOW_ACTION_ID.LOCK)).toBe(0);
            expect(byId.get(WORKFLOW_ACTION_ID.UNLOCK)).toBe(0);
        });

        it('should count Unlock only for locked items', () => {
            const items = [
                contentlet({ inode: 'a', locked: true }),
                contentlet({ inode: 'b', locked: false }),
                contentlet({ inode: 'c', locked: true })
            ];

            const unlock = getQuickActions(items).find(
                (action) => action.id === WORKFLOW_ACTION_ID.UNLOCK
            );

            expect(unlock?.count).toBe(2);
            expect(unlock?.eligibleInodes).toEqual(['a', 'c']);
        });

        it('should warn on Unlock about locks held by other users', () => {
            // `contentEditable` is the server's answer to "is this locked by *me*?" — false on a
            // locked row means someone else holds it, and only an administrator can release it.
            const items = [
                contentlet({ inode: 'mine', locked: true, contentEditable: true }),
                contentlet({ inode: 'theirs', locked: true, contentEditable: false }),
                contentlet({ inode: 'also-theirs', locked: true, contentEditable: false })
            ];

            const unlock = getQuickActions(items).find(
                (action) => action.id === WORKFLOW_ACTION_ID.UNLOCK
            );

            // Every locked item is still fired — the server is the authority on who may unlock.
            expect(unlock?.count).toBe(3);
            expect(unlock?.warningCount).toBe(2);
            expect(unlock?.warningHint).toBeTruthy();
        });

        it('should not warn on Unlock when every lock is the current user’s own', () => {
            const items = [contentlet({ inode: 'mine', locked: true, contentEditable: true })];

            const unlock = getQuickActions(items).find(
                (action) => action.id === WORKFLOW_ACTION_ID.UNLOCK
            );

            expect(unlock?.warningCount).toBe(0);
        });

        it('should not warn on Unlock for an administrator, whatever the locks', () => {
            // `canLock` returns true for a CMS Admin before it ever looks at the lock owner, so an
            // admin releases every one of these. Warning them is noise about an outcome that will
            // not happen — and it was the only role the warning was ever addressed to.
            const items = [
                contentlet({ inode: 'theirs', locked: true, contentEditable: false }),
                contentlet({ inode: 'also-theirs', locked: true, contentEditable: false })
            ];

            const unlock = getQuickActions(items, { isAdmin: true }).find(
                (action) => action.id === WORKFLOW_ACTION_ID.UNLOCK
            );

            // Still fired over both — the warning was never a filter.
            expect(unlock?.count).toBe(2);
            expect(unlock?.eligibleInodes).toEqual(['theirs', 'also-theirs']);
            expect(unlock?.warningCount).toBe(0);
            expect(unlock?.warningHint).toBeUndefined();
        });

        it('should warn on Unlock while the admin flag is still unresolved', () => {
            // An unresolved flag arrives as `false` (the store's default), so it behaves like a
            // non-admin. Deliberate: the hint says a foreign lock *may* require administrator
            // permission, so over-warning is honest, whereas staying quiet would let a non-admin
            // fire with no heads-up at all.
            const items = [contentlet({ inode: 'theirs', locked: true, contentEditable: false })];

            const unlock = getQuickActions(items, { isAdmin: false }).find(
                (action) => action.id === WORKFLOW_ACTION_ID.UNLOCK
            );

            expect(unlock?.warningCount).toBe(1);
        });

        it('should not warn on actions other than Unlock', () => {
            const items = [contentlet({ inode: 'a', locked: true, contentEditable: false })];

            for (const action of getQuickActions(items)) {
                if (action.id !== WORKFLOW_ACTION_ID.UNLOCK) {
                    expect(action.warningCount).toBe(0);
                }
            }
        });

        it('should still list actions that apply to nothing, with a zero count', () => {
            // Already locked, so Lock applies to no item — but stays in the list so the dialog can
            // render it as non-selectable rather than dropping the row.
            const items = [contentlet({ inode: 'a', locked: true })];

            const byId = new Map(getQuickActions(items).map((action) => [action.id, action.count]));

            expect(byId.get(WORKFLOW_ACTION_ID.LOCK)).toBe(0);
        });

        it('should keep a fixed display order regardless of the selection', () => {
            const expected = [
                WORKFLOW_ACTION_ID.LOCK,
                WORKFLOW_ACTION_ID.UNLOCK,
                ADD_TO_BUNDLE_ACTION_ID,
                PUSH_PUBLISH_ACTION_ID,
                REFRESH_ACTION_ID
            ];

            expect(getQuickActions([contentlet({ inode: 'a' })]).map((a) => a.id)).toEqual(
                expected
            );
            expect(
                getQuickActions([contentlet({ inode: 'b', archived: true, live: true })]).map(
                    (a) => a.id
                )
            ).toEqual(expected);
            expect(
                getQuickActions([contentlet({ inode: 'c', locked: true })]).map((a) => a.id)
            ).toEqual(expected);
        });

        it('should leave Add to Bundle selectable', () => {
            // No longer pending: the bundle picker exists, so the row opens its configuration step.
            const addToBundle = getQuickActions([contentlet({ inode: 'a' })]).find(
                (action) => action.id === ADD_TO_BUNDLE_ACTION_ID
            );

            expect(addToBundle?.count).toBe(1);
        });

        it('should count Add to Bundle against every selected contentlet', () => {
            // A bundle accepts any asset, so state does not narrow it.
            const items = [
                contentlet({ inode: 'a', archived: true }),
                contentlet({ inode: 'b', live: true }),
                folder('f1')
            ];

            const addToBundle = getQuickActions(items).find(
                (action) => action.id === ADD_TO_BUNDLE_ACTION_ID
            );

            expect(addToBundle?.count).toBe(2);
        });

        it('should offer the same set of actions regardless of the selection', () => {
            const archived = getQuickActions([contentlet({ inode: 'a', archived: true })]);
            const live = getQuickActions([contentlet({ inode: 'b', live: true })]);

            expect(archived.map((action) => action.id)).toEqual(live.map((action) => action.id));
        });

        it('should expose the eligible inodes, matching the count', () => {
            const items = [
                contentlet({ inode: 'unlocked', locked: false }),
                contentlet({ inode: 'is-locked', locked: true }),
                folder('f1')
            ];

            const lock = getQuickActions(items).find(
                (action) => action.id === WORKFLOW_ACTION_ID.LOCK
            );

            expect(lock?.eligibleInodes).toEqual(['unlocked']);
            expect(lock?.count).toBe(lock?.eligibleInodes.length);
        });

        it('should keep count and eligibleInodes in step for every action', () => {
            const items = [
                contentlet({ inode: 'a', archived: true }),
                contentlet({ inode: 'b', live: true }),
                contentlet({ inode: 'c' })
            ];

            for (const action of getQuickActions(items)) {
                expect(action.count).toBe(action.eligibleInodes.length);
            }
        });

        it('should flag Refresh as coming soon', () => {
            const byId = new Map(
                getQuickActions([contentlet({ inode: 'a' })]).map((action) => [action.id, action])
            );

            expect(byId.get(REFRESH_ACTION_ID)?.comingSoon).toBe(true);
        });

        it('should block Push Publish on the environments, not on coming-soon', () => {
            // Nothing is missing from dotCMS here, something is missing from the configuration, and
            // the fix belongs to an administrator. The two states read differently on the row.
            const byId = new Map(
                getQuickActions([contentlet({ inode: 'a' })]).map((action) => [action.id, action])
            );

            expect(byId.get(PUSH_PUBLISH_ACTION_ID)?.comingSoon).toBe(false);
            expect(byId.get(PUSH_PUBLISH_ACTION_ID)?.missingEnvironments).toBe(true);
        });

        it('should release Push Publish once an environment is reachable', () => {
            const push = getQuickActions([contentlet({ inode: 'a' }), contentlet({ inode: 'b' })], {
                isAdmin: false,
                hasPushPublishEnvironments: true
            }).find((action) => action.id === PUSH_PUBLISH_ACTION_ID);

            expect(push?.missingEnvironments).toBe(false);
            // Every contentlet counts: no row state disqualifies a push.
            expect(push?.count).toBe(2);
        });

        it('should keep Push Publish blocked while the environments are still unknown', () => {
            // An unresolved lookup reads as "none" — enabling and then retracting is worse than a
            // row that stays shut until the answer arrives.
            const push = getQuickActions([contentlet({ inode: 'a' })], { isAdmin: false }).find(
                (action) => action.id === PUSH_PUBLISH_ACTION_ID
            );

            expect(push?.missingEnvironments).toBe(true);
        });

        it('should never block the other rows on the environments', () => {
            const byId = new Map(
                getQuickActions([contentlet({ inode: 'a', locked: true })]).map((action) => [
                    action.id,
                    action
                ])
            );

            for (const id of [
                WORKFLOW_ACTION_ID.UNLOCK,
                ADD_TO_BUNDLE_ACTION_ID,
                REFRESH_ACTION_ID
            ] as string[]) {
                expect(byId.get(id)?.missingEnvironments).toBe(false);
            }
        });

        it('should not flag the wired actions as coming soon', () => {
            const byId = new Map(
                getQuickActions([contentlet({ inode: 'a' })]).map((action) => [action.id, action])
            );

            expect(byId.get(WORKFLOW_ACTION_ID.LOCK)?.comingSoon).toBe(false);
            expect(byId.get(WORKFLOW_ACTION_ID.UNLOCK)?.comingSoon).toBe(false);
            expect(byId.get(ADD_TO_BUNDLE_ACTION_ID)?.comingSoon).toBe(false);
        });

        it('should count the coming-soon actions over the whole selection', () => {
            // A `0` would read as "does not apply to these items", which is a different claim from
            // "not built yet". The row is disabled by `comingSoon`, not by its count.
            const items = [
                contentlet({ inode: 'a', archived: true }),
                contentlet({ inode: 'b', live: true, locked: true }),
                folder('f1')
            ];

            const byId = new Map(getQuickActions(items).map((action) => [action.id, action.count]));

            expect(byId.get(PUSH_PUBLISH_ACTION_ID)).toBe(2);
            expect(byId.get(REFRESH_ACTION_ID)).toBe(2);
        });
    });

    describe('toActionCenterSchemes', () => {
        it('should return an empty array for an empty response', () => {
            expect(toActionCenterSchemes({ schemes: [] })).toEqual([]);
            expect(toActionCenterSchemes(undefined as unknown as DotBulkActionView)).toEqual([]);
        });

        it('should flatten steps into a single action list per scheme', () => {
            const view = bulkActionView('Editorial Workflow', [
                [2, [{ id: 'a1', name: 'Send for Review', count: 2 }]],
                [1, [{ id: 'a2', name: 'Translate', count: 1 }]]
            ]);

            const [scheme] = toActionCenterSchemes(view);

            expect(scheme.name).toBe('Editorial Workflow');
            expect(scheme.actions.map((action) => action.name)).toEqual([
                'Send for Review',
                'Translate'
            ]);
        });

        it('should sum step counts into the scheme count', () => {
            const view = bulkActionView('System Workflow', [
                [2, [{ id: 'a1', name: 'Publish', count: 3 }]],
                [1, [{ id: 'a2', name: 'Archive', count: 1 }]]
            ]);

            expect(toActionCenterSchemes(view)[0].count).toBe(3);
        });

        it('should dedupe an action appearing on several steps, keeping the highest count', () => {
            const view = bulkActionView('System Workflow', [
                [1, [{ id: 'same', name: 'Publish', count: 3 }]],
                [1, [{ id: 'same', name: 'Publish', count: 1 }]]
            ]);

            const [scheme] = toActionCenterSchemes(view);

            expect(scheme.actions).toHaveLength(1);
            expect(scheme.actions[0].count).toBe(3);
        });

        it('should drop schemes that expose no actions', () => {
            const view = bulkActionView('Empty Workflow', [[2, []]]);

            expect(toActionCenterSchemes(view)).toEqual([]);
        });

        it('should carry the input flags the API advertises', () => {
            const view = bulkActionView('System Workflow', [
                [
                    1,
                    [
                        { id: 'pp', name: 'Push Publish', count: 1, flags: { pushPublish: true } },
                        { id: 'mv', name: 'Move', count: 1, flags: { moveable: true } },
                        { id: 'as', name: 'Assign', count: 1, flags: { assignable: true } },
                        { id: 'cm', name: 'Comment', count: 1, flags: { commentable: true } },
                        { id: 'ok', name: 'Plain', count: 1 }
                    ]
                ]
            ]);

            const byId = new Map(
                toActionCenterSchemes(view)[0].actions.map((action) => [
                    action.id,
                    Object.values(action.inputs).some(Boolean)
                ])
            );

            expect(byId.get('pp')).toBe(true);
            expect(byId.get('mv')).toBe(true);
            expect(byId.get('as')).toBe(true);
            expect(byId.get('cm')).toBe(true);
            expect(byId.get('ok')).toBe(false);
        });

        it('should carry each input flag through individually', () => {
            // The roll-up is not enough: the dialog collects a move path but not the other three, so
            // it has to be able to tell which kind an action is asking for.
            const view = bulkActionView('System Workflow', [
                [
                    1,
                    [
                        {
                            id: 'combo',
                            name: 'Approve',
                            count: 1,
                            flags: { assignable: true, commentable: true, pushPublish: true }
                        },
                        { id: 'mv', name: 'Move', count: 1, flags: { moveable: true } }
                    ]
                ]
            ]);

            const byId = new Map(
                toActionCenterSchemes(view)[0].actions.map((action) => [action.id, action.inputs])
            );

            // One action, three inputs at once — the case a single boolean cannot express.
            expect(byId.get('combo')).toEqual({
                moveable: false,
                pushPublish: true,
                assignable: true,
                commentable: true
            });
            expect(byId.get('mv')).toEqual({
                moveable: true,
                pushPublish: false,
                assignable: false,
                commentable: false
            });
        });

        it('should flag a count as approximate when the action has a condition', () => {
            const view = bulkActionView('Editorial Workflow', [
                [
                    2,
                    [
                        {
                            id: 'tr',
                            name: 'Translate',
                            count: 2,
                            flags: { conditionPresent: true }
                        },
                        { id: 'sr', name: 'Send for Review', count: 2 }
                    ]
                ]
            ]);

            const byId = new Map(
                toActionCenterSchemes(view)[0].actions.map((action) => [
                    action.id,
                    action.approximateCount
                ])
            );

            expect(byId.get('tr')).toBe(true);
            expect(byId.get('sr')).toBe(false);
        });

        it('should tolerate a scheme with no steps array', () => {
            const view = {
                schemes: [{ scheme: { id: 's1', name: 'Broken' } }]
            } as unknown as DotBulkActionView;

            expect(toActionCenterSchemes(view)).toEqual([]);
        });

        it('should leave content types unresolved for an ungrouped lookup', () => {
            // Only `mergeActionCenterSchemes` knows which content type a response came from.
            const view = bulkActionView('System Workflow', [
                [1, [{ id: 'a1', name: 'Publish', count: 1 }]]
            ]);

            expect(toActionCenterSchemes(view)[0].actions[0].contentTypes).toEqual([]);
        });
    });

    describe('groupByContentType', () => {
        it('should return one entry per distinct content type', () => {
            const items = [
                contentlet({ inode: 'a', contentType: 'Blog' }),
                contentlet({ inode: 'b', contentType: 'VtlInclude' }),
                contentlet({ inode: 'c', contentType: 'Blog' })
            ] as DotCMSContentlet[];

            expect(groupByContentType(items)).toEqual([
                { contentType: 'Blog', contentlets: [items[0], items[2]] },
                { contentType: 'VtlInclude', contentlets: [items[1]] }
            ]);
        });

        it('should return nothing for an empty selection', () => {
            expect(groupByContentType([])).toEqual([]);
        });
    });

    describe('mergeActionCenterSchemes', () => {
        it('should stamp each action with the content type it came from', () => {
            const merged = mergeActionCenterSchemes([
                {
                    contentType: 'Blog',
                    view: bulkActionView('Blogs', [[1, [{ id: 'copy', name: 'Copy', count: 1 }]]])
                }
            ]);

            expect(merged[0].actions[0].contentTypes).toEqual(['Blog']);
        });

        it('should keep schemes from different content types side by side', () => {
            const merged = mergeActionCenterSchemes([
                {
                    contentType: 'Blog',
                    view: bulkActionView('Blogs', [[1, [{ id: 'copy', name: 'Copy', count: 1 }]]])
                },
                {
                    contentType: 'VtlInclude',
                    view: bulkActionView('Vtl', [[1, [{ id: 'reset', name: 'Reset', count: 1 }]]])
                }
            ]);

            expect(merged.map((scheme) => scheme.name)).toEqual(['Blogs', 'Vtl']);
        });

        it('should sum counts and union content types for a shared action', () => {
            // Two content types on the same scheme: each group counted its own contentlets, so the
            // totals add up to what one combined lookup would have reported.
            const merged = mergeActionCenterSchemes([
                {
                    contentType: 'Blog',
                    view: bulkActionView('System Workflow', [
                        [2, [{ id: 'publish', name: 'Publish', count: 2 }]]
                    ])
                },
                {
                    contentType: 'News',
                    view: bulkActionView('System Workflow', [
                        [3, [{ id: 'publish', name: 'Publish', count: 3 }]]
                    ])
                }
            ]);

            expect(merged).toHaveLength(1);
            expect(merged[0].count).toBe(5);
            expect(merged[0].actions).toHaveLength(1);
            expect(merged[0].actions[0].count).toBe(5);
            expect(merged[0].actions[0].contentTypes).toEqual(['Blog', 'News']);
        });

        it('should keep an action approximate when any group could not evaluate its condition', () => {
            const merged = mergeActionCenterSchemes([
                {
                    contentType: 'Blog',
                    view: bulkActionView('Editorial', [
                        [1, [{ id: 'tr', name: 'Translate', count: 1 }]]
                    ])
                },
                {
                    contentType: 'News',
                    view: bulkActionView('Editorial', [
                        [
                            1,
                            [
                                {
                                    id: 'tr',
                                    name: 'Translate',
                                    count: 1,
                                    flags: { conditionPresent: true }
                                }
                            ]
                        ]
                    ])
                }
            ]);

            expect(merged[0].actions[0].approximateCount).toBe(true);
        });

        it('should merge actions unique to one content type into the shared scheme', () => {
            const merged = mergeActionCenterSchemes([
                {
                    contentType: 'Blog',
                    view: bulkActionView('System Workflow', [
                        [1, [{ id: 'publish', name: 'Publish', count: 1 }]]
                    ])
                },
                {
                    contentType: 'News',
                    view: bulkActionView('System Workflow', [
                        [1, [{ id: 'archive', name: 'Archive', count: 1 }]]
                    ])
                }
            ]);

            expect(merged[0].actions.map((action) => action.id)).toEqual(['archive', 'publish']);
            expect(merged[0].actions.map((action) => action.contentTypes)).toEqual([
                ['News'],
                ['Blog']
            ]);
        });

        it('should return nothing for no groups', () => {
            expect(mergeActionCenterSchemes([])).toEqual([]);
        });
    });

    describe('eligibleContentlets', () => {
        const items = [
            contentlet({ inode: 'blog', contentType: 'Blog' }),
            contentlet({ inode: 'vtl', contentType: 'VtlInclude' })
        ] as DotCMSContentlet[];

        const action = (contentTypes: string[]): DotActionCenterWorkflowAction => ({
            id: 'a1',
            name: 'Copy',
            count: 1,
            inputs: NO_INPUTS,
            approximateCount: false,
            contentTypes
        });

        it('should keep only contentlets of the action content types', () => {
            expect(eligibleContentlets(action(['Blog']), items).map((item) => item.inode)).toEqual([
                'blog'
            ]);
        });

        it('should keep every matching content type', () => {
            expect(
                eligibleContentlets(action(['Blog', 'VtlInclude']), items).map((item) => item.inode)
            ).toEqual(['blog', 'vtl']);
        });

        it('should fall back to the whole selection when content types are unresolved', () => {
            // Showing too many rows is recoverable; showing none would look like a broken dialog.
            expect(eligibleContentlets(action([]), items)).toEqual(items);
        });

        it('should fall back to the whole selection when there is no action', () => {
            expect(eligibleContentlets(undefined, items)).toEqual(items);
        });

        it('should return nothing when no contentlet matches', () => {
            expect(eligibleContentlets(action(['Unrelated']), items)).toEqual([]);
        });
    });

    describe('requiredInputKinds', () => {
        it('should return nothing for an action that fires from the selection alone', () => {
            expect(requiredInputKinds(actionWithInputs({}))).toEqual([]);
        });

        it('should collapse assignable and commentable into one screen', () => {
            expect(
                requiredInputKinds(actionWithInputs({ assignable: true, commentable: true }))
            ).toEqual(['assignComment']);
        });

        it('should order the screens as the legacy wizard does', () => {
            // Assign/comment before push publish, so the sequence matches the old dialog.
            expect(
                requiredInputKinds(
                    actionWithInputs({ pushPublish: true, commentable: true, moveable: true })
                )
            ).toEqual(['move', 'assignComment', 'pushPublish']);
        });

        it('should return nothing when there is no action', () => {
            expect(requiredInputKinds(undefined)).toEqual([]);
        });

        it.each([
            ['a move path', { moveable: true }, ['move']],
            ['an assignee', { assignable: true }, ['assignComment']],
            ['a comment', { commentable: true }, ['assignComment']],
            ['push publish', { pushPublish: true }, ['pushPublish']]
        ])('should return one section for an action needing %s', (_label, inputs, expected) => {
            expect(requiredInputKinds(actionWithInputs(inputs))).toEqual(expected);
        });

        it.each([
            ['a move path and an assignee', { moveable: true, assignable: true }],
            ['push publish and a comment', { pushPublish: true, commentable: true }],
            ['a move path and push publish', { moveable: true, pushPublish: true }]
        ])('should return every section for an action needing %s', (_label, inputs) => {
            // Nothing is refused any more: all of them render together on one screen.
            expect(requiredInputKinds(actionWithInputs(inputs)).length).toBe(2);
        });
    });

    describe('toPathToMove', () => {
        it('should convert the picker value into the actionlet path format', () => {
            expect(toPathToMove('demo.dotcms.com:/application/containers')).toBe(
                '//demo.dotcms.com/application/containers'
            );
        });

        it('should handle a site root', () => {
            expect(toPathToMove('demo.dotcms.com:/')).toBe('//demo.dotcms.com/');
        });

        it('should insert the missing separator when the path has none', () => {
            expect(toPathToMove('demo.dotcms.com:application')).toBe(
                '//demo.dotcms.com/application'
            );
        });

        it.each([
            ['unset', undefined],
            ['null', null],
            ['empty', ''],
            // No hostname to build a path from — `//:/x` would be sent as a valid-looking path and
            // rejected server-side with an opaque error.
            ['a leading separator', ':/application'],
            ['no separator', 'demo.dotcms.com']
        ])('should return nothing for %s', (_label, value) => {
            expect(toPathToMove(value)).toBe('');
        });
    });

    describe('toHostFolderValue', () => {
        it('should convert the actionlet path into the picker format', () => {
            expect(toHostFolderValue('//demo.dotcms.com/application/containers')).toBe(
                'demo.dotcms.com:/application/containers'
            );
        });

        it('should convert a site root', () => {
            expect(toHostFolderValue('//demo.dotcms.com/')).toBe('demo.dotcms.com:/');
        });

        it('should treat a bare host as the site root', () => {
            expect(toHostFolderValue('//demo.dotcms.com')).toBe('demo.dotcms.com:/');
        });

        it.each([
            ['unset', undefined],
            ['null', null],
            ['empty', ''],
            ['a missing prefix', 'demo.dotcms.com/application'],
            ['only the prefix', '//']
        ])('should return nothing for %s', (_label, value) => {
            expect(toHostFolderValue(value)).toBe('');
        });

        it('should round-trip with toPathToMove', () => {
            // The two conversions bracket the picker, so a value that survives one has to survive
            // both — otherwise the seeded destination would differ from the one that gets sent.
            const path = '//demo.dotcms.com/application/containers';

            expect(toPathToMove(toHostFolderValue(path))).toBe(path);
        });
    });
});
