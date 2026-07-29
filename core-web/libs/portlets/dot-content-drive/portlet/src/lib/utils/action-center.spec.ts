import { describe, expect, it } from '@jest/globals';

import { DotBulkActionView, DotContentDriveItem } from '@dotcms/dotcms-models';

import {
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

        it('should offer Delete and Unarchive only for archived items', () => {
            const items = [
                contentlet({ inode: 'a', archived: true }),
                contentlet({ inode: 'b', archived: false })
            ];

            const actions = getQuickActions(items);
            const byId = new Map(actions.map((action) => [action.id, action.count]));

            expect(byId.get(WORKFLOW_ACTION_ID.DELETE)).toBe(1);
            expect(byId.get(WORKFLOW_ACTION_ID.UNARCHIVE)).toBe(1);
        });

        it('should omit actions that apply to nothing rather than showing a zero count', () => {
            // Nothing archived, so Delete and Unarchive should not be offered at all.
            const items = [contentlet({ inode: 'a', archived: false })];

            const ids = getQuickActions(items).map((action) => action.id);

            expect(ids).not.toContain(WORKFLOW_ACTION_ID.DELETE);
            expect(ids).not.toContain(WORKFLOW_ACTION_ID.UNARCHIVE);
        });

        it('should not count archived items as publishable', () => {
            const items = [contentlet({ inode: 'a', archived: true, live: false })];

            const ids = getQuickActions(items).map((action) => action.id);

            expect(ids).not.toContain(WORKFLOW_ACTION_ID.PUBLISH);
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
