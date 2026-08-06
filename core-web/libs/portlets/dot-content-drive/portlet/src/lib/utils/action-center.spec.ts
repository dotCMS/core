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
    mergeActionCenterSchemes,
    toActionCenterSchemes,
    toContentletInodes
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

    describe('getQuickActions', () => {
        it('should return no actions for an empty selection', () => {
            expect(getQuickActions([])).toEqual([]);
        });

        it('should return no actions for a folder-only selection', () => {
            expect(getQuickActions([folder('f1')])).toEqual([]);
        });

        it('should count Publish for items that are not live', () => {
            const items = [
                contentlet({ inode: 'a', live: false }),
                contentlet({ inode: 'b', live: true }),
                contentlet({ inode: 'c', live: false })
            ];

            const publish = getQuickActions(items).find(
                (action) => action.id === WORKFLOW_ACTION_ID.PUBLISH
            );

            expect(publish?.count).toBe(2);
        });

        it('should count Unpublish only for live items', () => {
            const items = [
                contentlet({ inode: 'a', live: true }),
                contentlet({ inode: 'b', live: false })
            ];

            const unpublish = getQuickActions(items).find(
                (action) => action.id === WORKFLOW_ACTION_ID.UNPUBLISH
            );

            expect(unpublish?.count).toBe(1);
        });

        it('should count Delete and Unarchive only for archived items', () => {
            const items = [
                contentlet({ inode: 'a', archived: true }),
                contentlet({ inode: 'b', archived: false })
            ];

            const byId = new Map(getQuickActions(items).map((action) => [action.id, action.count]));

            expect(byId.get(WORKFLOW_ACTION_ID.DELETE)).toBe(1);
            expect(byId.get(WORKFLOW_ACTION_ID.UNARCHIVE)).toBe(1);
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

        it('should not count archived items as lockable', () => {
            // An archived contentlet is always unlocked (archive refuses locked content), so it
            // would otherwise be counted — but locking it would only block Unarchive and Delete.
            const items = [contentlet({ inode: 'a', archived: true, locked: false })];

            const lock = getQuickActions(items).find(
                (action) => action.id === WORKFLOW_ACTION_ID.LOCK
            );

            expect(lock?.count).toBe(0);
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

        it('should not warn on actions other than Unlock', () => {
            const items = [contentlet({ inode: 'a', locked: true, contentEditable: false })];

            for (const action of getQuickActions(items)) {
                if (action.id !== WORKFLOW_ACTION_ID.UNLOCK) {
                    expect(action.warningCount).toBe(0);
                }
            }
        });

        it('should still list actions that apply to nothing, with a zero count', () => {
            // Nothing archived, so Delete applies to no item — but stays in the list so the dialog
            // can render it as non-selectable rather than dropping the row.
            const items = [contentlet({ inode: 'a', archived: false })];

            const byId = new Map(getQuickActions(items).map((action) => [action.id, action.count]));

            expect(byId.get(WORKFLOW_ACTION_ID.DELETE)).toBe(0);
        });

        it('should keep a fixed display order regardless of the selection', () => {
            const expected = [
                WORKFLOW_ACTION_ID.PUBLISH,
                WORKFLOW_ACTION_ID.UNPUBLISH,
                WORKFLOW_ACTION_ID.ARCHIVE,
                WORKFLOW_ACTION_ID.DELETE,
                WORKFLOW_ACTION_ID.UNARCHIVE,
                WORKFLOW_ACTION_ID.LOCK,
                WORKFLOW_ACTION_ID.UNLOCK,
                ADD_TO_BUNDLE_ACTION_ID
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

        it('should mark Add to Bundle as pending so it can never be fired', () => {
            const addToBundle = getQuickActions([contentlet({ inode: 'a' })]).find(
                (action) => action.id === ADD_TO_BUNDLE_ACTION_ID
            );

            expect(addToBundle?.pendingHint).toBeTruthy();
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

        it('should not count archived items as publishable', () => {
            const items = [contentlet({ inode: 'a', archived: true, live: false })];

            const publish = getQuickActions(items).find(
                (action) => action.id === WORKFLOW_ACTION_ID.PUBLISH
            );

            expect(publish?.count).toBe(0);
        });

        it('should expose the eligible inodes, matching the count', () => {
            const items = [
                contentlet({ inode: 'not-live', live: false }),
                contentlet({ inode: 'is-live', live: true }),
                folder('f1')
            ];

            const publish = getQuickActions(items).find(
                (action) => action.id === WORKFLOW_ACTION_ID.PUBLISH
            );

            expect(publish?.eligibleInodes).toEqual(['not-live']);
            expect(publish?.count).toBe(publish?.eligibleInodes.length);
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

        it('should mark destructive actions as danger', () => {
            const items = [contentlet({ inode: 'a', archived: true })];

            const remove = getQuickActions(items).find(
                (action) => action.id === WORKFLOW_ACTION_ID.DELETE
            );

            expect(remove?.danger).toBe(true);
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

        it('should flag actions needing extra input', () => {
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
                    action.requiresInput
                ])
            );

            expect(byId.get('pp')).toBe(true);
            expect(byId.get('mv')).toBe(true);
            expect(byId.get('as')).toBe(true);
            expect(byId.get('cm')).toBe(true);
            expect(byId.get('ok')).toBe(false);
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
            requiresInput: false,
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
});
