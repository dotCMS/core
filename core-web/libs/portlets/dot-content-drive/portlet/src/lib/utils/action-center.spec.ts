import { describe, expect, it } from '@jest/globals';

import { DotBulkActionView, DotContentDriveItem } from '@dotcms/dotcms-models';

import {
    ADD_TO_BUNDLE_ACTION_ID,
    excludeFolders,
    getQuickActions,
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

        it('should count Delete only for archived items', () => {
            const items = [
                contentlet({ inode: 'a', archived: true }),
                contentlet({ inode: 'b', archived: false })
            ];

            const byId = new Map(getQuickActions(items).map((action) => [action.id, action.count]));

            expect(byId.get(WORKFLOW_ACTION_ID.DELETE)).toBe(1);
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
    });
});
