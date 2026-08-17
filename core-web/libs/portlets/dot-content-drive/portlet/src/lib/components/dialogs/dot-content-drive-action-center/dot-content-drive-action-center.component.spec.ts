import { afterEach, beforeEach, describe, expect, it, jest } from '@jest/globals';
import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';

import { ConfirmationService } from 'primeng/api';

import {
    AddToBundleService,
    DotCurrentUserService,
    DotFormatDateService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotMessageService,
    DotRolesService,
    DotWorkflowsActionsService,
    PushPublishService
} from '@dotcms/data-access';
import { DotcmsConfigService } from '@dotcms/dotcms-js';
import { DotBulkActionView, DotCMSSystemAction, DotContentDriveItem } from '@dotcms/dotcms-models';
import { DotBrowsingService, DotWorkflowAssignCommentComponent } from '@dotcms/ui';
import { DotcmsConfigServiceMock } from '@dotcms/utils-testing';

import { DotContentDriveActionCenterComponent } from './dot-content-drive-action-center.component';

import { DotContentDriveActionExecution } from '../../../shared/models';
import { DotContentDriveStore } from '../../../store/dot-content-drive.store';

const contentlet = (
    overrides: Partial<DotContentDriveItem> & { inode: string }
): DotContentDriveItem =>
    ({
        baseType: 'CONTENT',
        // A real content type matters here: the bulk lookup is grouped by it, and actions carry the
        // content types that can run them.
        contentType: 'Blog',
        title: `Title ${overrides.inode}`,
        live: false,
        working: true,
        archived: false,
        locked: false,
        ...overrides
    }) as DotContentDriveItem;

const folder = (inode: string): DotContentDriveItem =>
    ({ type: 'folder', inode, identifier: inode }) as unknown as DotContentDriveItem;

const BULK_ACTIONS_RESPONSE = {
    schemes: [
        {
            scheme: { id: 'editorial', name: 'Editorial Workflow' },
            steps: [
                {
                    step: {
                        count: 2,
                        workflowStep: { id: 'step-1', name: 'Draft', schemeId: 'editorial' }
                    },
                    actions: [
                        {
                            count: 2,
                            pushPublish: false,
                            moveable: false,
                            conditionPresent: false,
                            workflowAction: {
                                id: 'action-review',
                                name: 'Send for Review',
                                assignable: false,
                                commentable: false
                            }
                        },
                        {
                            count: 2,
                            pushPublish: true,
                            moveable: false,
                            conditionPresent: false,
                            workflowAction: {
                                id: 'action-pp',
                                name: 'Push Publish',
                                assignable: false,
                                commentable: false
                            }
                        },
                        {
                            count: 2,
                            pushPublish: false,
                            moveable: true,
                            conditionPresent: false,
                            workflowAction: {
                                id: 'action-move',
                                name: 'Move',
                                assignable: false,
                                commentable: false
                            }
                        },
                        {
                            // Assignable and commentable, which is still one screen.
                            count: 2,
                            pushPublish: false,
                            moveable: false,
                            conditionPresent: false,
                            workflowAction: {
                                id: 'action-assign',
                                name: 'Send to Legal',
                                assignable: true,
                                commentable: true,
                                nextAssign: 'role-legal',
                                roleHierarchyForAssign: true
                            }
                        },
                        {
                            // Two screens' worth, which is what stays disabled.
                            count: 2,
                            pushPublish: true,
                            moveable: false,
                            conditionPresent: false,
                            workflowAction: {
                                id: 'action-approve',
                                name: 'Approve and Push',
                                assignable: true,
                                commentable: false
                            }
                        }
                    ]
                }
            ]
        }
    ]
} as DotBulkActionView;

/**
 * Scheme-level system action mappings for `editorial`, pointing at actions the bulk response above
 * reports with a non-zero count — so both gates pass and the gated quick actions are live.
 *
 * `LOCK`/`UNLOCK` are deliberately absent: they have no actionlet and can never be mapped, which is
 * why those rows are exempt rather than gated.
 */
const SCHEME_SYSTEM_ACTIONS = ['PUBLISH', 'UNPUBLISH', 'ARCHIVE', 'UNARCHIVE', 'DELETE'].map(
    (systemAction) => ({
        identifier: `mapping-${systemAction}`,
        systemAction,
        workflowAction: { id: 'action-review', name: 'Send for Review', schemeId: 'editorial' },
        ownerContentType: false,
        ownerScheme: true
    })
) as unknown as DotCMSSystemAction[];

/**
 * What the fire carries when the action declared no inputs.
 *
 * Every slot is sent on every fire — the request shape is identical either way, and the server reads
 * only the parts the action asked for.
 */
/** A complete push publish payload, in the shape the step emits. */
const PUSH_PUBLISH_SETTINGS = {
    whereToSend: 'env-1',
    iWantTo: 'publish' as const,
    publishDate: '2026-08-12',
    publishTime: '10-30',
    expireDate: '2026-08-12',
    expireTime: '10-30',
    filterKey: 'filter-b',
    timezoneId: 'Europe/Madrid'
};

const NO_INPUTS_SENT = {
    pathToMove: '',
    assignComment: { assign: '', comment: '' },
    pushPublish: undefined
};

describe('DotContentDriveActionCenterComponent', () => {
    let spectator: Spectator<DotContentDriveActionCenterComponent>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;
    let workflowsActionsService: SpyObject<DotWorkflowsActionsService>;
    let confirmationService: SpyObject<ConfirmationService>;

    const mockSelectedItems = signal<DotContentDriveItem[]>([]);
    // Owned by the store now, so the dialog reads it rather than tracking its own executing flag.
    const mockActionExecution = signal<DotContentDriveActionExecution | undefined>(undefined);
    // Resolved once on portlet init, so the dialog reads it rather than fetching per open. `false`
    // is both the non-admin case and the still-loading one — see `isLockedByAnotherUser`.
    const mockCurrentUserIsAdmin = signal<boolean>(false);
    // Where Content Drive is browsing. Only the hostname and path matter to this dialog.
    const mockCurrentSite = signal<{ hostname: string } | undefined>({
        hostname: 'demo.dotcms.com'
    });
    const mockPath = signal<string>('/blogs');

    const createComponent = createComponentFactory({
        component: DotContentDriveActionCenterComponent,
        providers: [
            provideHttpClient(),
            mockProvider(DotContentDriveStore, {
                selectedItems: mockSelectedItems,
                actionExecution: mockActionExecution,
                currentUserIsAdmin: mockCurrentUserIsAdmin,
                // The folder being browsed, which seeds the move destination picker.
                currentSite: mockCurrentSite,
                path: mockPath,
                loadItems: jest.fn(),
                setStatus: jest.fn(),
                setSelectedItems: jest.fn(),
                closeDialog: jest.fn(),
                setDialogDrillDown: jest.fn(),
                clearDialogDrillDown: jest.fn(),
                executeQuickAction: jest.fn(),
                executeWorkflowAction: jest.fn(),
                executeAddToBundle: jest.fn()
            }),
            mockProvider(DotMessageService, {
                get: jest.fn().mockImplementation((key: string) => key)
            }),
            // Pulled in by the Content Drive grid, which the action preview renders for real.
            mockProvider(DotLanguagesService, { get: jest.fn(() => of([])) }),
            // `DotcmsConfigServiceMock` has no `getTimeZones` (it lives in a separate mock), and the
            // push publish step loads timezones on init.
            mockProvider(DotcmsConfigService, {
                ...new DotcmsConfigServiceMock(),
                getTimeZones: jest.fn(() => of([]))
            }),
            // Backs the env selector embedded in the push publish step.
            mockProvider(PushPublishService, {
                getEnvironments: jest.fn(() => of([])),
                lastEnvironmentPushed: null
            }),
            mockProvider(DotRolesService, { get: jest.fn(() => of([])) }),
            mockProvider(DotFormatDateService),
            // Pulled in by the folder picker's own store, which the move configuration step renders
            // for real. Mocked rather than stubbed out with a fake child component: whether that
            // component can actually be instantiated inside this dialog is the thing worth proving.
            mockProvider(DotHttpErrorManagerService),
            // Backs the bundle step's list of the current user's unsent bundles.
            mockProvider(AddToBundleService, { getBundles: jest.fn(() => of([])) }),
            mockProvider(DotCurrentUserService),
            mockProvider(DotBrowsingService, {
                getSitesTreePath: jest.fn(() => of([])),
                getSitesPage: jest.fn(() => of({ sites: [], total: 0 })),
                // Returns a real node, not `null`. This is what makes the picker's
                // `ControlValueAccessor` actually push a value outward on load — the emission that
                // silently overwrote a chosen destination when the step was re-created. A `null` here
                // makes the whole class of bug invisible to these tests.
                getCurrentSiteAsTreeNodeItem: jest.fn(() =>
                    of({
                        key: 'site-1',
                        label: 'demo.dotcms.com',
                        data: {
                            id: 'site-1',
                            hostname: 'demo.dotcms.com',
                            path: '',
                            type: 'site'
                        },
                        leaf: false
                    })
                )
            })
        ],
        detectChanges: false
    });

    beforeEach(() => {
        mockSelectedItems.set([
            contentlet({ inode: 'inode-1' }),
            contentlet({ inode: 'inode-2', live: true })
        ]);
        mockActionExecution.set(undefined);
        mockCurrentUserIsAdmin.set(false);
        mockCurrentSite.set({ hostname: 'demo.dotcms.com' });
        mockPath.set('/blogs');

        spectator = createComponent();

        store = spectator.inject(DotContentDriveStore, true);
        workflowsActionsService = spectator.inject(DotWorkflowsActionsService, true);
        confirmationService = spectator.inject(ConfirmationService, true);

        jest.spyOn(workflowsActionsService, 'getBulkActions').mockReturnValue(
            of(BULK_ACTIONS_RESPONSE)
        );
        // No content-type-level override; the scheme's mappings are what resolve — the shape of a
        // stock install, where only the scheme carries Default Actions.
        jest.spyOn(workflowsActionsService, 'getSystemActionsByContentType').mockReturnValue(
            of([])
        );
        jest.spyOn(workflowsActionsService, 'getSystemActionsByScheme').mockReturnValue(
            of(SCHEME_SYSTEM_ACTIONS)
        );
        jest.spyOn(store, 'closeDialog');
        jest.spyOn(store, 'loadItems');
        // Records the call without accepting, so tests opt in to the accept path explicitly.
        jest.spyOn(confirmationService, 'confirm').mockReturnValue(confirmationService);
    });

    afterEach(() => {
        // The store mock is shared across tests; without this, call counts accumulate and
        // "should not have been called" assertions see calls from earlier tests.
        jest.clearAllMocks();
    });

    /** Renders the dialog and arms the plain (no extra input) workflow action. */
    const armAction = (): void => {
        spectator.detectChanges();
        spectator.component['$selectedActionId'].set('action-review');
        spectator.detectChanges();
    };

    /** Arms the action and drills into its preview. */
    const goToPreview = (): void => {
        armAction();
        spectator.click('[data-testid="action-center-continue"]');
        spectator.detectChanges();
    };

    /** Opens a quick action's preview. Renders first, so callers can set the selection beforehand. */
    const openQuickActionPreview = (id: string): void => {
        spectator.detectChanges();
        spectator.click(`[data-testid="quick-action-${id}"]`);
        spectator.detectChanges();
    };

    /** Opens a quick action's preview and commits it, the full two-step path a user takes. */
    const executeQuickAction = (id: string): void => {
        openQuickActionPreview(id);
        spectator.click('[data-testid="action-preview-execute"]');
        spectator.detectChanges();
    };

    /** Every row the preview is listing. The preview renders the Content Drive grid. */
    const previewRows = (): HTMLElement[] => spectator.queryAll('[data-testid="item-row"]');

    /** Clicks a preview row's real checkbox, toggling it in or out of the included set. */
    const toggleRow = (index: number): void => {
        spectator.click(previewRows()[index].querySelector('input'));
        spectator.detectChanges();
    };

    /** Drops the first preview row from the included set. */
    const uncheckFirstRow = (): void => toggleRow(0);

    /**
     * Rows whose lock belongs to another user, marked in the preview so the user can drop them.
     *
     * The point of routing quick actions through a preview at all: the Unlock row warns that some
     * locks are not the user's, and this is the only place that says *which*.
     */
    describe('marking locks held by another user', () => {
        const lockedSelection = [
            contentlet({ inode: 'mine', locked: true, contentEditable: true }),
            contentlet({ inode: 'theirs', locked: true, contentEditable: false })
        ];

        it('should mark only the rows the current user does not hold', () => {
            mockSelectedItems.set(lockedSelection);

            openQuickActionPreview('UNLOCK');

            expect(spectator.queryAll('[data-testid="lock-foreign-icon"]').length).toBe(1);
            expect(spectator.queryAll('[data-testid="lock-icon"]').length).toBe(1);
        });

        it('should mark foreign locks on any action’s preview, not just Unlock', () => {
            // A lock held by somebody else fails a Publish or an Archive just as readily, so the
            // marker is not scoped to Unlock. Both rows here are unpublished, so Publish applies to
            // each of them and only the foreign lock is marked.
            mockSelectedItems.set(lockedSelection);

            openQuickActionPreview('PUBLISH');

            expect(previewRows().length).toBe(2);
            expect(spectator.queryAll('[data-testid="lock-foreign-icon"]').length).toBe(1);
        });

        it('should mark nothing for an administrator', () => {
            // Same signal as the row's warning: an admin releases every lock, so neither the
            // warning nor the markers appear for them.
            mockCurrentUserIsAdmin.set(true);
            mockSelectedItems.set(lockedSelection);

            openQuickActionPreview('UNLOCK');

            expect(spectator.queryAll('[data-testid="lock-foreign-icon"]').length).toBe(0);
            expect(spectator.queryAll('[data-testid="lock-icon"]').length).toBe(2);
        });

        it('should keep marked rows checked, so the fired count matches what the row advertised', () => {
            mockSelectedItems.set(lockedSelection);

            executeQuickAction('UNLOCK');

            expect(store.executeQuickAction).toHaveBeenCalledWith('UNLOCK', expect.any(String), [
                'mine',
                'theirs'
            ]);
        });

        it('should drop a marked row from the payload once the user unchecks it', () => {
            // The capability this screen exists for, end to end: see which locks are not yours,
            // uncheck one, and fire without it.
            mockSelectedItems.set(lockedSelection);
            openQuickActionPreview('UNLOCK');

            const markedRow = previewRows().findIndex((row) =>
                row.querySelector('[data-testid="lock-foreign-icon"]')
            );
            toggleRow(markedRow);

            spectator.click('[data-testid="action-preview-execute"]');
            spectator.detectChanges();

            expect(store.executeQuickAction).toHaveBeenCalledWith('UNLOCK', expect.any(String), [
                'mine'
            ]);
        });
    });

    describe('loading the available actions', () => {
        it('should request bulk actions with the selected inodes', () => {
            spectator.detectChanges();

            expect(workflowsActionsService.getBulkActions).toHaveBeenCalledWith({
                contentletIds: ['inode-1', 'inode-2']
            });
        });

        it('should exclude folders from the request', () => {
            mockSelectedItems.set([contentlet({ inode: 'inode-1' }), folder('folder-1')]);

            spectator.detectChanges();

            expect(workflowsActionsService.getBulkActions).toHaveBeenCalledWith({
                contentletIds: ['inode-1']
            });
        });

        it('should not call the endpoint when the selection is folders only', () => {
            mockSelectedItems.set([folder('folder-1'), folder('folder-2')]);

            spectator.detectChanges();

            expect(workflowsActionsService.getBulkActions).not.toHaveBeenCalled();
        });

        it('should render one panel per scheme', () => {
            spectator.detectChanges();

            expect(spectator.query('[data-testid="workflow-schemes"]')).toBeTruthy();
            expect(spectator.query('[data-testid="no-workflow-actions"]')).toBeFalsy();
        });

        it('should show the empty state when no scheme exposes actions', () => {
            jest.spyOn(workflowsActionsService, 'getBulkActions').mockReturnValue(
                of({ schemes: [] })
            );

            spectator.detectChanges();

            expect(spectator.query('[data-testid="no-workflow-actions"]')).toBeTruthy();
        });

        it('should show an inline error when the lookup fails', () => {
            jest.spyOn(workflowsActionsService, 'getBulkActions').mockReturnValue(
                throwError(() => new Error('boom'))
            );

            spectator.detectChanges();

            expect(spectator.query('[data-testid="workflow-actions-error"]')).toBeTruthy();
        });
    });

    describe('folders in the selection', () => {
        it('should warn that folders are ignored', () => {
            mockSelectedItems.set([contentlet({ inode: 'inode-1' }), folder('folder-1')]);

            spectator.detectChanges();

            expect(spectator.query('[data-testid="folders-ignored-message"]')).toBeTruthy();
        });

        it('should not warn when the selection has no folders', () => {
            spectator.detectChanges();

            expect(spectator.query('[data-testid="folders-ignored-message"]')).toBeFalsy();
        });

        it('should render the notice statically, with no entrance animation', () => {
            // The notice is present the moment the dialog opens, and PrimeNG's Message animates its
            // own height from zero over 300ms with no way to opt out through the component — which
            // read as the notice arriving late and shoving the action list down. `no-enter-motion` is
            // what the component's styles hook onto to suppress it.
            mockSelectedItems.set([contentlet({ inode: 'inode-1' }), folder('folder-1')]);

            spectator.detectChanges();

            const notice = spectator.query('[data-testid="folders-ignored-message"]');

            expect(notice).toBeTruthy();
            expect(notice?.classList.contains('no-enter-motion')).toBe(true);
        });
    });

    describe('quick actions', () => {
        it('should render a quick action for the eligible subset', () => {
            spectator.detectChanges();

            // inode-1 is not live, so Publish applies to exactly one item.
            expect(spectator.query('[data-testid="quick-action-PUBLISH"]')).toBeTruthy();
        });

        it('should render actions that apply to nothing as non-selectable', () => {
            // Nothing locked, so Unlock applies to no item — the row stays, disabled. Unlock rather
            // than Delete because the gated rows no longer carry a row-state filter at all.
            spectator.detectChanges();

            const unlock = spectator.query(
                '[data-testid="quick-action-UNLOCK"]'
            ) as HTMLButtonElement;

            expect(unlock).toBeTruthy();
            expect(unlock.disabled).toBe(true);
        });

        it('should keep applicable actions selectable', () => {
            spectator.detectChanges();

            const publish = spectator.query(
                '[data-testid="quick-action-PUBLISH"]'
            ) as HTMLButtonElement;

            expect(publish.disabled).toBe(false);
        });

        it('should keep Add to Bundle selectable', () => {
            spectator.detectChanges();

            const addToBundle = spectator.query(
                '[data-testid="quick-action-ADD_TO_BUNDLE"]'
            ) as HTMLButtonElement;

            expect(addToBundle).toBeTruthy();
            expect(addToBundle.disabled).toBe(false);
        });

        it('should open the bundle step rather than the preview for Add to Bundle', () => {
            // The one quick action that cannot fire from the selection alone: every other row goes
            // straight to its preview.
            spectator.detectChanges();

            spectator.click('[data-testid="quick-action-ADD_TO_BUNDLE"]');
            spectator.detectChanges();

            expect(spectator.query('[data-testid="action-configure-bundle-target"]')).toBeTruthy();
            expect(spectator.query('[data-testid="action-preview"]')).toBeNull();
            expect(store.executeQuickAction).not.toHaveBeenCalled();
        });

        it('should confirm before firing Delete, then fire on accept', () => {
            // Delete only applies to archived items, and only it carries a confirmMessage.
            mockSelectedItems.set([contentlet({ inode: 'inode-1', archived: true })]);
            jest.spyOn(confirmationService, 'confirm').mockImplementation((config) => {
                config.accept?.();

                return confirmationService;
            });

            executeQuickAction('DELETE');

            expect(confirmationService.confirm).toHaveBeenCalled();
            expect(store.executeQuickAction).toHaveBeenCalledWith('DELETE', expect.any(String), [
                'inode-1'
            ]);
        });

        it('should not fire Delete when the confirmation is dismissed', () => {
            mockSelectedItems.set([contentlet({ inode: 'inode-1', archived: true })]);
            // Default mock records the call without invoking `accept`.
            executeQuickAction('DELETE');

            expect(confirmationService.confirm).toHaveBeenCalled();
            expect(store.executeQuickAction).not.toHaveBeenCalled();
        });

        it('should fire non-destructive actions without confirming', () => {
            executeQuickAction('PUBLISH');

            expect(confirmationService.confirm).not.toHaveBeenCalled();
            expect(store.executeQuickAction).toHaveBeenCalled();
        });

        it('should not fire an action that applies to nothing', () => {
            spectator.detectChanges();

            spectator.click('[data-testid="quick-action-UNLOCK"]');
            spectator.detectChanges();

            // Never even reaches the preview.
            expect(spectator.query('[data-testid="action-preview"]')).toBeNull();
            expect(store.executeQuickAction).not.toHaveBeenCalled();
        });

        it('should fire only the inodes the action applies to, not the whole selection', () => {
            // Lock keeps its state filter, so it still narrows: inode-2 is already locked.
            mockSelectedItems.set([
                contentlet({ inode: 'inode-1' }),
                contentlet({ inode: 'inode-2', locked: true })
            ]);

            executeQuickAction('LOCK');

            expect(store.executeQuickAction).toHaveBeenCalledWith('LOCK', expect.any(String), [
                'inode-1'
            ]);
        });

        it('should fire a gated action over the whole mapped selection', () => {
            // No row-state filter any more: what Publish means is the mapping's business, and both
            // rows are the same mapped content type.
            executeQuickAction('PUBLISH');

            expect(store.executeQuickAction).toHaveBeenCalledWith('PUBLISH', expect.any(String), [
                'inode-1',
                'inode-2'
            ]);
        });

        it('should fire exactly as many inodes as the row advertises', () => {
            // Guards the count/payload pair against drifting apart again: whatever number the row
            // shows must equal the number of inodes sent.
            spectator.detectChanges();

            const row = spectator.query('[data-testid="quick-action-PUBLISH"]');
            const advertised = Number(row?.textContent?.match(/\((\d+)\)/)?.[1]);

            executeQuickAction('PUBLISH');

            const [, , inodes] = (store.executeQuickAction as unknown as jest.Mock).mock
                .calls[0] as [string, string, string[]];

            expect(advertised).toBe(2);
            expect(inodes).toHaveLength(advertised);
        });

        it('should not narrow Delete by archived state', () => {
            // Delete used to be offered for archived rows only. That filter assumed deleting was the
            // whole effect of the system action, which a mapping makes untrue.
            //
            // NOTE for manual QA: `ESContentletAPIImpl` still refuses to delete unarchived content
            // that has more than one language version, so those rows come back as per-item failures
            // in the result toast rather than being filtered out here.
            mockSelectedItems.set([
                contentlet({ inode: 'archived-1', archived: true }),
                contentlet({ inode: 'live-1', live: true })
            ]);
            jest.spyOn(confirmationService, 'confirm').mockImplementation((config) => {
                config.accept?.();

                return confirmationService;
            });

            executeQuickAction('DELETE');

            expect(store.executeQuickAction).toHaveBeenCalledWith('DELETE', expect.any(String), [
                'archived-1',
                'live-1'
            ]);
        });

        it('should close the dialog as soon as the run is handed to the store', () => {
            // The dialog is modal, so leaving it open would dim the toolbar that reports the run —
            // and the counts it shows are stale the moment contentlets start moving step.
            executeQuickAction('PUBLISH');

            expect(store.executeQuickAction).toHaveBeenCalled();
            expect(store.closeDialog).toHaveBeenCalled();
        });
    });

    describe('lock and unlock quick actions', () => {
        it('should fire LOCK with only the unlocked inodes', () => {
            mockSelectedItems.set([
                contentlet({ inode: 'unlocked-1', locked: false }),
                contentlet({ inode: 'locked-1', locked: true })
            ]);

            executeQuickAction('LOCK');

            expect(store.executeQuickAction).toHaveBeenCalledWith('LOCK', expect.any(String), [
                'unlocked-1'
            ]);
        });

        it('should fire UNLOCK with only the locked inodes', () => {
            mockSelectedItems.set([
                contentlet({ inode: 'unlocked-1', locked: false }),
                contentlet({ inode: 'locked-1', locked: true })
            ]);

            executeQuickAction('UNLOCK');

            expect(store.executeQuickAction).toHaveBeenCalledWith('UNLOCK', expect.any(String), [
                'locked-1'
            ]);
        });

        it('should keep Unlock non-selectable when nothing in the selection is locked', () => {
            mockSelectedItems.set([contentlet({ inode: 'unlocked-1', locked: false })]);

            spectator.detectChanges();

            const unlock = spectator.query(
                '[data-testid="quick-action-UNLOCK"]'
            ) as HTMLButtonElement;

            expect(unlock.disabled).toBe(true);
        });

        it('should keep Lock non-selectable when everything is already locked', () => {
            mockSelectedItems.set([contentlet({ inode: 'locked-1', locked: true })]);

            spectator.detectChanges();

            const lock = spectator.query('[data-testid="quick-action-LOCK"]') as HTMLButtonElement;

            expect(lock.disabled).toBe(true);
        });

        it('should flag on the Unlock row how many locks are held by other users', () => {
            mockSelectedItems.set([
                contentlet({ inode: 'mine', locked: true, contentEditable: true }),
                contentlet({ inode: 'theirs', locked: true, contentEditable: false })
            ]);

            spectator.detectChanges();

            expect(spectator.query('[data-testid="quick-action-warning-UNLOCK"]')).toBeTruthy();
        });

        it('should not flag the Unlock row when every lock is the current user’s own', () => {
            mockSelectedItems.set([
                contentlet({ inode: 'mine', locked: true, contentEditable: true })
            ]);

            spectator.detectChanges();

            expect(spectator.query('[data-testid="quick-action-warning-UNLOCK"]')).toBeNull();
        });

        it('should not flag the Unlock row for an administrator', () => {
            // The warning exists to tell a user their unlock may be refused. An admin's never is,
            // so for the one role that could act on it the warning is pure noise.
            mockCurrentUserIsAdmin.set(true);
            mockSelectedItems.set([
                contentlet({ inode: 'theirs', locked: true, contentEditable: false })
            ]);

            spectator.detectChanges();

            expect(spectator.query('[data-testid="quick-action-warning-UNLOCK"]')).toBeNull();
        });

        it('should drop the Unlock warning when the admin flag resolves late', () => {
            // The flag starts `false` and is patched in when `getCurrentUser` answers, which can
            // land after the dialog has rendered. `$quickActions` reads it as a signal so the row
            // recomputes rather than keeping the warning it opened with.
            mockSelectedItems.set([
                contentlet({ inode: 'theirs', locked: true, contentEditable: false })
            ]);

            spectator.detectChanges();

            expect(spectator.query('[data-testid="quick-action-warning-UNLOCK"]')).toBeTruthy();

            mockCurrentUserIsAdmin.set(true);
            spectator.detectChanges();

            expect(spectator.query('[data-testid="quick-action-warning-UNLOCK"]')).toBeNull();
        });

        it('should still fire every locked item for an administrator', () => {
            // The warning going quiet must not change the payload: Unlock still attempts all of
            // them, the same set a non-admin would have fired.
            mockCurrentUserIsAdmin.set(true);
            mockSelectedItems.set([
                contentlet({ inode: 'mine', locked: true, contentEditable: true }),
                contentlet({ inode: 'theirs', locked: true, contentEditable: false })
            ]);

            executeQuickAction('UNLOCK');

            expect(store.executeQuickAction).toHaveBeenCalledWith('UNLOCK', expect.any(String), [
                'mine',
                'theirs'
            ]);
        });

        it('should still fire every locked item when some are held by other users', () => {
            // Attempt-all is deliberate: the client cannot know whether the user holds the CMS
            // Administrator role that lets them release someone else's lock.
            mockSelectedItems.set([
                contentlet({ inode: 'mine', locked: true, contentEditable: true }),
                contentlet({ inode: 'theirs', locked: true, contentEditable: false })
            ]);

            executeQuickAction('UNLOCK');

            expect(store.executeQuickAction).toHaveBeenCalledWith('UNLOCK', expect.any(String), [
                'mine',
                'theirs'
            ]);
        });

        it('should exclude folders from Lock', () => {
            mockSelectedItems.set([
                contentlet({ inode: 'unlocked-1', locked: false }),
                folder('folder-1')
            ]);

            executeQuickAction('LOCK');

            expect(store.executeQuickAction).toHaveBeenCalledWith('LOCK', expect.any(String), [
                'unlocked-1'
            ]);
        });

        it('should hand unlock to the store with only the locked inodes', () => {
            mockSelectedItems.set([contentlet({ inode: 'locked-1', locked: true })]);

            executeQuickAction('UNLOCK');

            expect(store.executeQuickAction).toHaveBeenCalledWith('UNLOCK', expect.any(String), [
                'locked-1'
            ]);
        });

        it('should not confirm before locking or unlocking', () => {
            mockSelectedItems.set([contentlet({ inode: 'locked-1', locked: true })]);

            executeQuickAction('UNLOCK');

            expect(confirmationService.confirm).not.toHaveBeenCalled();
        });
    });

    describe('the workflow mapping gate', () => {
        it('should keep gated actions live when the scheme maps them', () => {
            spectator.detectChanges();

            const publish = spectator.query(
                '[data-testid="quick-action-PUBLISH"]'
            ) as HTMLButtonElement;

            expect(publish.disabled).toBe(false);
        });

        it('should shut a gated action when nothing maps it', () => {
            jest.spyOn(workflowsActionsService, 'getSystemActionsByScheme').mockReturnValue(of([]));

            spectator.detectChanges();

            const publish = spectator.query(
                '[data-testid="quick-action-PUBLISH"]'
            ) as HTMLButtonElement;

            expect(publish.disabled).toBe(true);
        });

        it('should leave the exempt actions untouched when nothing maps anything', () => {
            // The rule the split exists to make visible: these have no mapping to gate on.
            jest.spyOn(workflowsActionsService, 'getSystemActionsByScheme').mockReturnValue(of([]));
            mockSelectedItems.set([contentlet({ inode: 'inode-1', locked: true })]);

            spectator.detectChanges();

            for (const id of ['UNLOCK', 'ADD_TO_BUNDLE']) {
                expect(
                    (spectator.query(`[data-testid="quick-action-${id}"]`) as HTMLButtonElement)
                        .disabled
                ).toBe(false);
            }
        });

        it('should ask for the mappings of every content type and scheme in play', () => {
            spectator.detectChanges();

            expect(workflowsActionsService.getSystemActionsByContentType).toHaveBeenCalledWith(
                'Blog'
            );
            expect(workflowsActionsService.getSystemActionsByScheme).toHaveBeenCalledWith(
                'editorial'
            );
        });

        it('should shut the gated actions when the mapping lookup fails', () => {
            // Fails closed. The workflow actions are unaffected, so the dialog stays usable.
            jest.spyOn(workflowsActionsService, 'getSystemActionsByScheme').mockReturnValue(
                throwError(() => new Error('boom'))
            );

            spectator.detectChanges();

            expect(
                (spectator.query('[data-testid="quick-action-PUBLISH"]') as HTMLButtonElement)
                    .disabled
            ).toBe(true);
            expect(spectator.query('[data-testid="workflow-schemes"]')).toBeTruthy();
        });

        it('should fire a gated action on the mapped content type only', () => {
            mockSelectedItems.set([
                contentlet({ inode: 'blog-1', contentType: 'Blog' }),
                contentlet({ inode: 'banner-1', contentType: 'Banner' })
            ]);
            // Banner's lookup comes back with nothing available, so PUBLISH resolves for Blog alone.
            jest.spyOn(workflowsActionsService, 'getBulkActions').mockImplementation((request) =>
                of(
                    (request.contentletIds ?? []).includes('blog-1')
                        ? BULK_ACTIONS_RESPONSE
                        : { schemes: [] }
                )
            );

            executeQuickAction('PUBLISH');

            expect(store.executeQuickAction).toHaveBeenCalledWith('PUBLISH', expect.any(String), [
                'blog-1'
            ]);
        });

        it('should account for the excluded rows in the row tooltip, without a second marker', () => {
            // One warning per row: a dedicated icon here meant two tooltips on the same row, and
            // they overlapped each other on hover.
            mockSelectedItems.set([
                contentlet({ inode: 'blog-1', contentType: 'Blog' }),
                contentlet({ inode: 'banner-1', contentType: 'Banner' })
            ]);
            jest.spyOn(workflowsActionsService, 'getBulkActions').mockImplementation((request) =>
                of(
                    (request.contentletIds ?? []).includes('blog-1')
                        ? BULK_ACTIONS_RESPONSE
                        : { schemes: [] }
                )
            );

            spectator.detectChanges();

            const publish = spectator.component['$quickActions']().find(
                (action) => action.id === 'PUBLISH'
            )!;

            expect(spectator.query('[data-testid="quick-action-unmapped-PUBLISH"]')).toBeNull();
            expect(publish.unmappedCount).toBe(1);
            expect(spectator.component['quickActionHint'](publish)).toBe(
                'content-drive.action-center.partly-mapped'
            );
        });

        it('should render one list with the ungatable rows leading it', () => {
            // One list, not two groups. The three rows no mapping can gate come first, so the top of
            // the list does not move with the selection or the content type.
            spectator.detectChanges();

            const ids = spectator
                .queryAll('[data-testid="quick-actions-list"] button')
                .map((row) => row.getAttribute('data-testid'));

            expect(ids.slice(0, 3)).toEqual([
                'quick-action-LOCK',
                'quick-action-UNLOCK',
                'quick-action-ADD_TO_BUNDLE'
            ]);
            expect(ids).toContain('quick-action-PUBLISH');
        });

        it('should explain the gate with an info link', () => {
            spectator.detectChanges();

            const info = spectator.query('[data-testid="quick-actions-info"]');

            expect(info).toBeTruthy();
            expect(info?.getAttribute('href')).toBeTruthy();
            expect(info?.getAttribute('target')).toBe('_blank');
            expect(info?.getAttribute('rel')).toContain('noopener');
        });
    });

    describe('quick action preview', () => {
        it('should open the preview instead of firing when a quick action is clicked', () => {
            openQuickActionPreview('PUBLISH');

            expect(spectator.query('[data-testid="action-preview"]')).toBeTruthy();
            expect(spectator.query('[data-testid="action-center"]')).toBeNull();
            expect(store.executeQuickAction).not.toHaveBeenCalled();
        });

        it('should list only the contentlets the quick action applies to', () => {
            // inode-2 is already locked, so Lock applies to inode-1 only and the preview must not
            // offer inode-2 as something the user could include.
            mockSelectedItems.set([
                contentlet({ inode: 'inode-1' }),
                contentlet({ inode: 'inode-2', locked: true })
            ]);

            openQuickActionPreview('LOCK');

            const rows = previewRows();

            expect(rows).toHaveLength(1);
            expect(rows[0].textContent).toContain('Title inode-1');
        });

        it('should retitle the dialog header with the quick action and its count', () => {
            openQuickActionPreview('PUBLISH');

            expect(store.setDialogDrillDown).toHaveBeenCalledWith({
                header: 'Default-Action-Publish',
                itemCount: 2
            });
        });

        it('should fire only the rows left checked in the preview', () => {
            mockSelectedItems.set([
                contentlet({ inode: 'keep-me' }),
                contentlet({ inode: 'drop-me' })
            ]);

            openQuickActionPreview('PUBLISH');
            uncheckFirstRow();
            spectator.click('[data-testid="action-preview-execute"]');
            spectator.detectChanges();

            expect(store.executeQuickAction).toHaveBeenCalledWith('PUBLISH', expect.any(String), [
                'drop-me'
            ]);
        });

        it('should keep Execute disabled once every row is unchecked', () => {
            mockSelectedItems.set([contentlet({ inode: 'only-one' })]);

            openQuickActionPreview('PUBLISH');
            uncheckFirstRow();

            const execute = spectator.query(
                '[data-testid="action-preview-execute"] button'
            ) as HTMLButtonElement;

            expect(execute.disabled).toBe(true);
        });

        it('should return to the action list without firing when Back is clicked', () => {
            openQuickActionPreview('PUBLISH');
            spectator.click('[data-testid="action-preview-back"]');
            spectator.detectChanges();

            expect(spectator.query('[data-testid="action-center"]')).toBeTruthy();
            expect(store.executeQuickAction).not.toHaveBeenCalled();
            expect(store.clearDialogDrillDown).toHaveBeenCalled();
        });

        it('should confirm at Execute rather than when the destructive row is clicked', () => {
            // The commit point moved: opening a preview changes nothing, so prompting there would
            // ask the user to confirm something that has not been decided yet.
            mockSelectedItems.set([contentlet({ inode: 'inode-1', archived: true })]);

            openQuickActionPreview('DELETE');

            expect(confirmationService.confirm).not.toHaveBeenCalled();

            spectator.click('[data-testid="action-preview-execute"]');
            spectator.detectChanges();

            expect(confirmationService.confirm).toHaveBeenCalled();
        });

        it('should not show the workflow partial-match warning on a quick action', () => {
            // That warning explains a backend count falling short of the rows shown. A quick
            // action's count is derived from the rows themselves, so it can never fall short.
            openQuickActionPreview('PUBLISH');

            expect(spectator.query('[data-testid="action-preview-partial-match"]')).toBeNull();
        });

        it('should let the user drop locks held by other users before unlocking', () => {
            // The reason a preview earns its place on Unlock: the row-level warning becomes
            // actionable here, because the user can exclude the items that would fail.
            mockSelectedItems.set([
                contentlet({ inode: 'mine', locked: true, contentEditable: true }),
                contentlet({ inode: 'theirs', locked: true, contentEditable: false })
            ]);

            openQuickActionPreview('UNLOCK');
            uncheckFirstRow();
            spectator.click('[data-testid="action-preview-execute"]');
            spectator.detectChanges();

            expect(store.executeQuickAction).toHaveBeenCalledWith('UNLOCK', expect.any(String), [
                'theirs'
            ]);
        });
    });

    describe('workflow actions', () => {
        it('should keep Continue disabled until an action is selected', () => {
            spectator.detectChanges();

            // PrimeNG puts `disabled` on the inner <button>, not on the p-button host.
            const continueButton = spectator.query(
                '[data-testid="action-center-continue"] button'
            ) as HTMLButtonElement;

            expect(continueButton.disabled).toBe(true);
        });

        it('should open the preview instead of firing when Continue is clicked', () => {
            armAction();

            spectator.click('[data-testid="action-center-continue"]');
            spectator.detectChanges();

            expect(spectator.query('[data-testid="action-preview"]')).toBeTruthy();
            expect(spectator.query('[data-testid="action-center"]')).toBeNull();
            expect(store.executeWorkflowAction).not.toHaveBeenCalled();
        });

        it('should not fire when no action is selected', () => {
            spectator.detectChanges();

            spectator.component['onExecuteWorkflowAction']();

            expect(store.executeWorkflowAction).not.toHaveBeenCalled();
        });

        it('should not open the preview when no action is selected', () => {
            spectator.detectChanges();

            spectator.component['onContinueToPreview']();
            spectator.detectChanges();

            expect(spectator.query('[data-testid="action-preview"]')).toBeNull();
        });

        it.each([
            ['nothing', 'action-review'],
            ['a move path', 'action-move'],
            ['an assignee and a comment', 'action-assign'],
            ['push publish', 'action-pp'],
            // The row that used to be greyed: two sections now render together.
            ['both an assignee and push publish', 'action-approve']
        ])('should enable an action needing %s', (_label, actionId) => {
            // No input gate remains: whatever an action declares gets a section on the configuration
            // screen, so every row is armable.
            spectator.detectChanges();

            const row = spectator.query(
                `[data-testid="workflow-action-${actionId}"] input`
            ) as HTMLInputElement;

            expect(row.disabled).toBe(false);
        });

        it('should offer no requires-input hint on any row', () => {
            spectator.detectChanges();

            // Scoped to the workflow section: the Quick Actions heading carries its own
            // `pi-info-circle` explaining the workflow gate, which is not a row hint.
            expect(
                spectator.queryAll(
                    '[data-testid="workflow-actions-section"] .pi-info-circle[aria-hidden]'
                ).length
            ).toBe(
                // Only the approximate-count icons remain; none of the fixture's actions carry a
                // condition, so there should be none at all.
                0
            );
        });

        it('should not open the preview when the action applies to nothing', () => {
            // `eligibleContentlets` can come back empty if the selection changed between the lookup
            // and the click, which would otherwise open a preview with no rows to fire.
            armAction();
            mockSelectedItems.set([contentlet({ inode: 'other', contentType: 'Unrelated' })]);
            spectator.detectChanges();

            spectator.component['onContinueToPreview']();
            spectator.detectChanges();

            expect(spectator.query('[data-testid="action-preview"]')).toBeNull();
            expect(store.setDialogDrillDown).not.toHaveBeenCalled();
        });

        describe('switching scheme panels', () => {
            it('should disarm the action when a different scheme is opened', () => {
                // Execution is one action at a time, so a panel the user has navigated away from
                // must not leave its action armed.
                armAction();

                spectator.component['onOpenSchemeChange']('another-scheme');
                spectator.detectChanges();

                expect(spectator.component['$selectedActionId']()).toBeNull();
            });

            it('should disable Continue once the action is disarmed', () => {
                armAction();
                spectator.component['onOpenSchemeChange']('another-scheme');
                spectator.detectChanges();

                const continueButton = spectator.query(
                    '[data-testid="action-center-continue"] button'
                ) as HTMLButtonElement;

                expect(continueButton.disabled).toBe(true);
            });

            it('should disarm the action when every panel is collapsed', () => {
                armAction();

                spectator.component['onOpenSchemeChange'](undefined);
                spectator.detectChanges();

                expect(spectator.component['$selectedActionId']()).toBeNull();
            });

            it('should keep the action armed when its own scheme is reopened', () => {
                armAction();

                spectator.component['onOpenSchemeChange']('editorial');
                spectator.detectChanges();

                expect(spectator.component['$selectedActionId']()).toBe('action-review');
            });

            it('should take the first value when the accordion emits an array', () => {
                // PrimeNG's accordion `valueChange` can emit either a single value or an array, and
                // JSDOM clicking never produces the array shape. Two entries on purpose: a
                // single-entry array stringifies to the same thing as the bare value, so it would
                // pass even if the array handling were dropped.
                armAction();

                spectator.component['onOpenSchemeChange'](['editorial', 'another-scheme']);
                spectator.detectChanges();

                expect(spectator.component['$openSchemeId']()).toBe('editorial');
                expect(spectator.component['$selectedActionId']()).toBe('action-review');
            });
        });
    });

    describe('workflow action preview', () => {
        beforeEach(() => goToPreview());

        it('should list every selected contentlet, all included', () => {
            expect(previewRows().length).toBe(2);
            expect(spectator.component['$includedCount']()).toBe(2);
        });

        it('should retitle the dialog header rather than render its own', () => {
            // The dialog header belongs to the shell; publishing it through the store keeps one
            // header instead of the dialog's title and a second one in this body.
            expect(store.setDialogDrillDown).toHaveBeenCalledWith({
                header: 'Send for Review',
                itemCount: 2
            });
            expect(spectator.query('[data-testid="action-preview-title"]')).toBeNull();
        });

        it('should label Execute without repeating the action name', () => {
            // The dialog header already says "Send for Review", so naming it on the button too was
            // redundant and made the button grow with the action name.
            const execute = spectator.query('[data-testid="action-preview-execute"]');

            expect(execute.textContent).toContain('Execute');
            expect(execute.textContent).not.toContain('Send for Review');
        });

        it('should keep the published header count in step with the checked rows', () => {
            uncheckFirstRow();

            expect(store.setDialogDrillDown).toHaveBeenLastCalledWith({
                header: 'Send for Review',
                itemCount: 1
            });
        });

        it('should restore the dialog header when going back', () => {
            spectator.click('[data-testid="action-preview-back"]');
            spectator.detectChanges();

            expect(store.clearDialogDrillDown).toHaveBeenCalled();
        });

        it('should fire every included contentlet', () => {
            spectator.click('[data-testid="action-preview-execute"]');

            expect(store.executeWorkflowAction).toHaveBeenCalledWith(
                'action-review',
                expect.any(String),
                ['inode-1', 'inode-2'],
                // This action declares no inputs, so every slot goes out empty and the server
                // ignores them.
                NO_INPUTS_SENT
            );
        });

        it('should fire only the contentlets still checked', () => {
            // The whole point of the preview: unchecking a row must remove exactly that inode from
            // the payload. Before this screen existed the fire always sent the full selection.
            uncheckFirstRow();

            spectator.click('[data-testid="action-preview-execute"]');

            expect(store.executeWorkflowAction).toHaveBeenCalledWith(
                'action-review',
                expect.any(String),
                ['inode-2'],
                NO_INPUTS_SENT
            );
        });

        it('should keep the fired payload in step with the count shown on the button', () => {
            uncheckFirstRow();

            const badge = spectator.query('[data-testid="action-preview-execute"] .p-badge');
            spectator.click('[data-testid="action-preview-execute"]');

            const [, , contentletIds] = (store.executeWorkflowAction as unknown as jest.Mock).mock
                .calls[0] as [string, string, string[]];

            expect(contentletIds.length).toBe(Number(badge.textContent.trim()));
        });

        it('should disable Execute once nothing is included', () => {
            spectator.component['onIncludedItemsChange']([]);
            spectator.detectChanges();

            const execute = spectator.query(
                '[data-testid="action-preview-execute"] button'
            ) as HTMLButtonElement;

            expect(execute.disabled).toBe(true);
        });

        it('should not fire when nothing is included', () => {
            spectator.component['onIncludedItemsChange']([]);
            spectator.detectChanges();

            spectator.component['onExecuteWorkflowAction']();

            expect(store.executeWorkflowAction).not.toHaveBeenCalled();
        });

        it('should close the dialog as soon as the run is handed to the store', () => {
            spectator.click('[data-testid="action-preview-execute"]');

            expect(store.executeWorkflowAction).toHaveBeenCalled();
            expect(store.closeDialog).toHaveBeenCalled();
        });

        describe('back', () => {
            it('should return to the actions view with the action still armed', () => {
                spectator.click('[data-testid="action-preview-back"]');
                spectator.detectChanges();

                expect(spectator.query('[data-testid="action-center"]')).toBeTruthy();
                expect(spectator.query('[data-testid="action-preview"]')).toBeNull();
                // Kept on purpose: re-entering the preview must not mean re-picking the action.
                expect(spectator.component['$selectedActionId']()).toBe('action-review');
                expect(store.executeWorkflowAction).not.toHaveBeenCalled();
            });

            it('should be inert while an action is in flight', () => {
                // Driven from store state, not a local flag: a run started before this dialog
                // instance existed must still lock the view.
                mockActionExecution.set({ actionName: 'Send for Review', total: 2 });
                spectator.detectChanges();

                spectator.component['onBackToActions']();
                spectator.detectChanges();

                expect(spectator.query('[data-testid="action-preview"]')).toBeTruthy();
            });
        });
    });

    describe('move configuration step', () => {
        /** Arms the move action and drills in, which stops on the configuration step. */
        const goToConfigure = (): void => {
            spectator.detectChanges();
            spectator.component['$selectedActionId'].set('action-move');
            spectator.detectChanges();
            spectator.click('[data-testid="action-center-continue"]');
            spectator.detectChanges();
        };

        /**
         * Picks a destination, standing in for the step component's `pathToMoveChange`.
         *
         * Driven through the output rather than the real picker: that component owns an HTTP-backed
         * sites/folders store, and the `hostname:/path` → `//hostname/path` conversion it performs on
         * the way out is unit-tested in `action-center.spec.ts`. What matters here is what the dialog
         * does with an already-chosen path.
         */
        const chooseDestination = (pathToMove = '//demo.dotcms.com/application'): void => {
            spectator.component['onPathToMoveChange'](pathToMove);
            spectator.detectChanges();
        };

        beforeEach(() => {
            mockSelectedItems.set([
                contentlet({ inode: 'inode-1' }),
                contentlet({ inode: 'inode-2' })
            ]);
        });

        it('should stop on the configuration step instead of the preview', () => {
            goToConfigure();

            expect(spectator.query('[data-testid="action-configure-move-target"]')).toBeTruthy();
            expect(spectator.query('[data-testid="action-preview"]')).toBeNull();
        });

        it('should title the dialog header with the action and the item count', () => {
            // The count has to survive into this step: it is the only place it appears, since the
            // step deliberately does not repeat the row list.
            goToConfigure();

            expect(store.setDialogDrillDown).toHaveBeenLastCalledWith({
                header: 'Move',
                itemCount: 2
            });
        });

        it('should seed the destination with the folder being browsed', () => {
            // So the picker opens on the current location rather than the bare site list.
            goToConfigure();

            expect(spectator.component['$pathToMove']()).toBe('//demo.dotcms.com/blogs');
        });

        it('should warn but not block when the destination is the folder being browsed', () => {
            // Advisory, not a gate. It compares against the *browsing* path, and with a search or
            // filter applied the selection need not live there — blocking refused legitimate moves
            // of filtered results to the site root.
            goToConfigure();

            const continueButton = spectator.query(
                '[data-testid="action-configure-continue"] button'
            ) as HTMLButtonElement;

            expect(continueButton.disabled).toBe(false);
            expect(spectator.query('[data-testid="action-configure-warning"]')).toBeTruthy();
        });

        it('should drop the warning once a different destination is chosen', () => {
            goToConfigure();
            chooseDestination();

            expect(spectator.query('[data-testid="action-configure-warning"]')).toBeNull();
        });

        it('should keep Continue disabled when the destination is cleared', () => {
            goToConfigure();
            chooseDestination('');

            const continueButton = spectator.query(
                '[data-testid="action-configure-continue"] button'
            ) as HTMLButtonElement;

            expect(continueButton.disabled).toBe(true);
        });

        it('should allow firing a move to the folder being browsed', () => {
            // Wasteful, but the user's call — and the alternative blocked real moves. See the
            // warning test above.
            goToConfigure();

            spectator.click('[data-testid="action-configure-continue"]');
            spectator.detectChanges();
            spectator.click('[data-testid="action-preview-execute"]');

            expect(store.executeWorkflowAction).toHaveBeenCalledWith(
                'action-move',
                'Move',
                ['inode-1', 'inode-2'],
                expect.objectContaining({ pathToMove: '//demo.dotcms.com/blogs' })
            );
        });

        it('should enable Continue once a destination is chosen', () => {
            goToConfigure();
            chooseDestination();

            const continueButton = spectator.query(
                '[data-testid="action-configure-continue"] button'
            ) as HTMLButtonElement;

            expect(continueButton.disabled).toBe(false);
        });

        it('should reach the preview once a destination is chosen', () => {
            goToConfigure();
            chooseDestination();

            spectator.click('[data-testid="action-configure-continue"]');
            spectator.detectChanges();

            expect(spectator.query('[data-testid="action-preview"]')).toBeTruthy();
            expect(previewRows().length).toBe(2);
        });

        it('should fire with the chosen destination', () => {
            goToConfigure();
            chooseDestination();
            spectator.click('[data-testid="action-configure-continue"]');
            spectator.detectChanges();

            spectator.click('[data-testid="action-preview-execute"]');

            expect(store.executeWorkflowAction).toHaveBeenCalledWith(
                'action-move',
                'Move',
                ['inode-1', 'inode-2'],
                { ...NO_INPUTS_SENT, pathToMove: '//demo.dotcms.com/application' }
            );
        });

        it('should honour rows unchecked after the destination was chosen', () => {
            goToConfigure();
            chooseDestination();
            spectator.click('[data-testid="action-configure-continue"]');
            spectator.detectChanges();

            uncheckFirstRow();
            spectator.click('[data-testid="action-preview-execute"]');

            expect(store.executeWorkflowAction).toHaveBeenCalledWith(
                'action-move',
                'Move',
                ['inode-2'],
                { ...NO_INPUTS_SENT, pathToMove: '//demo.dotcms.com/application' }
            );
        });

        it('should refuse to fire a move with no destination', () => {
            // Reachable only by skipping the guarded Continue, but the cost of getting here is a
            // run where every item fails, so the commit point refuses it too.
            goToConfigure();
            chooseDestination('');
            spectator.component['$view'].set('preview');
            spectator.detectChanges();

            spectator.click('[data-testid="action-preview-execute"]');

            expect(store.executeWorkflowAction).not.toHaveBeenCalled();
        });

        it('should step back from the preview to the configuration step', () => {
            goToConfigure();
            chooseDestination();
            spectator.click('[data-testid="action-configure-continue"]');
            spectator.detectChanges();

            spectator.click('[data-testid="action-preview-back"]');
            spectator.detectChanges();

            // Back to the picker, not past it to the action list — otherwise correcting a
            // destination would mean re-picking the action and re-trimming the rows.
            expect(spectator.query('[data-testid="action-configure-move-target"]')).toBeTruthy();
            expect(spectator.query('[data-testid="action-center"]')).toBeNull();
        });

        it('should keep the chosen destination when stepping back to correct it', () => {
            goToConfigure();
            chooseDestination();
            spectator.click('[data-testid="action-configure-continue"]');
            spectator.detectChanges();

            spectator.click('[data-testid="action-preview-back"]');
            spectator.detectChanges();

            expect(spectator.component['$pathToMove']()).toBe('//demo.dotcms.com/application');
        });

        it('should drop the destination when returning to the action list', () => {
            // A path belongs to the run being set up; carrying it into another action's step would
            // pre-fill a decision never made for that action.
            goToConfigure();
            chooseDestination();

            spectator.click('[data-testid="action-configure-back"]');
            spectator.detectChanges();

            expect(spectator.component['$pathToMove']()).toBe('');
            expect(store.clearDialogDrillDown).toHaveBeenCalled();
        });

        it('should keep the configuration step inert while an action is in flight', () => {
            goToConfigure();
            chooseDestination();
            mockActionExecution.set({ actionName: 'Move', total: 2 });
            spectator.detectChanges();

            spectator.component['onContinueFromConfigure']();
            spectator.detectChanges();

            expect(spectator.query('[data-testid="action-configure-move-target"]')).toBeTruthy();
        });
    });

    describe('mixed content types', () => {
        /** A scheme whose actions only apply to the given content type's contentlets. */
        const schemeFor = (schemeId: string, actionId: string, actionName: string) =>
            ({
                schemes: [
                    {
                        scheme: { id: schemeId, name: schemeId },
                        steps: [
                            {
                                step: {
                                    count: 1,
                                    workflowStep: {
                                        id: 'step-1',
                                        name: 'Draft',
                                        schemeId
                                    }
                                },
                                actions: [
                                    {
                                        count: 1,
                                        pushPublish: false,
                                        moveable: false,
                                        conditionPresent: false,
                                        workflowAction: {
                                            id: actionId,
                                            name: actionName,
                                            assignable: false,
                                            commentable: false
                                        }
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }) as DotBulkActionView;

        beforeEach(() => {
            mockSelectedItems.set([
                contentlet({ inode: 'blog-1', contentType: 'Blog' }),
                contentlet({ inode: 'vtl-1', contentType: 'VtlInclude' })
            ]);

            // Schemes are assigned per content type, so each group gets a different response.
            jest.spyOn(workflowsActionsService, 'getBulkActions').mockImplementation((request) =>
                of(
                    request.contentletIds?.includes('blog-1')
                        ? schemeFor('Blogs', 'copy-blog', 'Copy Blog')
                        : schemeFor('Vtl', 'reset-vtl', 'Reset Workflow')
                )
            );
        });

        it('should ask the endpoint once per content type', () => {
            spectator.detectChanges();

            expect(workflowsActionsService.getBulkActions).toHaveBeenCalledTimes(2);
            expect(workflowsActionsService.getBulkActions).toHaveBeenCalledWith({
                contentletIds: ['blog-1']
            });
            expect(workflowsActionsService.getBulkActions).toHaveBeenCalledWith({
                contentletIds: ['vtl-1']
            });
        });

        it('should offer the actions of every content type in the selection', () => {
            spectator.detectChanges();

            expect(spectator.query('[data-testid="workflow-action-copy-blog"]')).toBeTruthy();
            expect(spectator.query('[data-testid="workflow-action-reset-vtl"]')).toBeTruthy();
        });

        it('should preview only the contentlets the action can run on', () => {
            // The bug this fixes: a Blog-only action used to list every selected contentlet,
            // including types its scheme is not assigned to, which the server was always going to
            // skip.
            spectator.detectChanges();
            spectator.component['$selectedActionId'].set('copy-blog');
            spectator.detectChanges();
            spectator.click('[data-testid="action-center-continue"]');
            spectator.detectChanges();

            const rows = previewRows();

            expect(rows.length).toBe(1);
            // Identified by the title the row renders rather than an inode attribute: the grid
            // carries no per-row identity attribute, and the title is what the user reads anyway.
            expect(rows[0].querySelector('[data-testid="item-title-text"]').textContent).toContain(
                'Title blog-1'
            );
        });

        it('should fire only the eligible contentlet', () => {
            spectator.detectChanges();
            spectator.component['$selectedActionId'].set('copy-blog');
            spectator.detectChanges();
            spectator.click('[data-testid="action-center-continue"]');
            spectator.detectChanges();

            spectator.click('[data-testid="action-preview-execute"]');

            expect(store.executeWorkflowAction).toHaveBeenCalledWith(
                'copy-blog',
                expect.any(String),
                ['blog-1'],
                NO_INPUTS_SENT
            );
        });

        it('should count the header against the eligible contentlets only', () => {
            spectator.detectChanges();
            spectator.component['$selectedActionId'].set('copy-blog');
            spectator.detectChanges();
            spectator.click('[data-testid="action-center-continue"]');
            spectator.detectChanges();

            expect(store.setDialogDrillDown).toHaveBeenCalledWith({
                header: 'Copy Blog',
                itemCount: 1
            });
        });

        it('should not warn about a partial match when every previewed row is eligible', () => {
            // Count is 1 and exactly 1 row is previewed, so nothing gets skipped.
            spectator.detectChanges();
            spectator.component['$selectedActionId'].set('copy-blog');
            spectator.detectChanges();
            spectator.click('[data-testid="action-center-continue"]');
            spectator.detectChanges();

            expect(spectator.query('[data-testid="action-preview-partial-match"]')).toBeNull();
        });

        it('should disarm an action when the user opens the other content type scheme', () => {
            // Two real schemes here, so this exercises the deselection against actual response data
            // rather than a made-up scheme id.
            spectator.detectChanges();
            spectator.component['$selectedActionId'].set('copy-blog');
            spectator.detectChanges();

            spectator.component['onOpenSchemeChange']('Vtl');
            spectator.detectChanges();

            expect(spectator.component['$selectedActionId']()).toBeNull();
        });

        it('should surface the lookup failure when any content type request fails', () => {
            jest.spyOn(workflowsActionsService, 'getBulkActions').mockReturnValue(
                throwError(() => new HttpErrorResponse({ status: 500 }))
            );

            spectator.detectChanges();

            expect(spectator.query('[data-testid="workflow-actions-error"]')).toBeTruthy();
        });
    });

    describe('partial match warning', () => {
        it('should stay hidden when the action covers the whole selection', () => {
            // The fixture's `action-review` has a count of 2 against a 2-contentlet selection.
            goToPreview();

            expect(spectator.query('[data-testid="action-preview-partial-match"]')).toBeNull();
        });

        it('should warn when the action covers fewer items than are selected', () => {
            // Three contentlets selected, but the action's backend count is still 2 — one of them
            // sits on a step this action does not belong to and will be skipped server-side.
            mockSelectedItems.set([
                contentlet({ inode: 'inode-1' }),
                contentlet({ inode: 'inode-2', live: true }),
                contentlet({ inode: 'inode-3' })
            ]);

            goToPreview();

            expect(spectator.query('[data-testid="action-preview-partial-match"]')).toBeTruthy();
        });
    });

    describe('add to bundle', () => {
        const BUNDLE = { id: 'bundle-1', name: 'Release 1' };

        /** Opens the bundle configuration step from the quick action row. */
        const goToBundleStep = (): void => {
            spectator.detectChanges();
            spectator.click('[data-testid="quick-action-ADD_TO_BUNDLE"]');
            spectator.detectChanges();
        };

        /** Picks a bundle, standing in for the step component's `bundleChange`. */
        const chooseBundle = (bundle = BUNDLE): void => {
            spectator.component['onBundleChange'](bundle);
            spectator.detectChanges();
        };

        it('should keep Continue disabled until a bundle is chosen', () => {
            goToBundleStep();

            const continueButton = spectator.query(
                '[data-testid="action-configure-continue"] button'
            ) as HTMLButtonElement;

            expect(continueButton.disabled).toBe(true);
        });

        it('should reach the preview once a bundle is chosen', () => {
            goToBundleStep();
            chooseBundle();

            spectator.click('[data-testid="action-configure-continue"]');
            spectator.detectChanges();

            expect(spectator.query('[data-testid="action-preview"]')).toBeTruthy();
        });

        it('should queue identifiers rather than inodes', () => {
            // The only action here that does: a bundle holds one entry per identifier.
            mockSelectedItems.set([
                contentlet({ inode: 'inode-1', identifier: 'id-1' }),
                contentlet({ inode: 'inode-2', identifier: 'id-2' })
            ]);
            goToBundleStep();
            chooseBundle();
            spectator.click('[data-testid="action-configure-continue"]');
            spectator.detectChanges();

            spectator.click('[data-testid="action-preview-execute"]');

            expect(store.executeAddToBundle).toHaveBeenCalledWith(expect.any(String), BUNDLE, [
                'id-1',
                'id-2'
            ]);
        });

        it('should collapse language versions of the same content into one asset', () => {
            // Two rows, one identifier: the endpoint dedupes anyway, and sending both would let the
            // dialog promise two assets when the result will honestly report one.
            mockSelectedItems.set([
                contentlet({ inode: 'inode-en', identifier: 'id-1' }),
                contentlet({ inode: 'inode-es', identifier: 'id-1' })
            ]);
            goToBundleStep();
            chooseBundle();
            spectator.click('[data-testid="action-configure-continue"]');
            spectator.detectChanges();

            spectator.click('[data-testid="action-preview-execute"]');

            expect(store.executeAddToBundle).toHaveBeenCalledWith(expect.any(String), BUNDLE, [
                'id-1'
            ]);
        });

        it('should not fire a workflow action', () => {
            // Add to Bundle is not a SystemAction and does not touch a workflow endpoint.
            goToBundleStep();
            chooseBundle();
            spectator.click('[data-testid="action-configure-continue"]');
            spectator.detectChanges();

            spectator.click('[data-testid="action-preview-execute"]');

            expect(store.executeQuickAction).not.toHaveBeenCalled();
            expect(store.executeWorkflowAction).not.toHaveBeenCalled();
        });

        it('should refuse to fire without a bundle', () => {
            goToBundleStep();
            spectator.component['$view'].set('preview');
            spectator.detectChanges();

            spectator.click('[data-testid="action-preview-execute"]');

            expect(store.executeAddToBundle).not.toHaveBeenCalled();
        });

        it('should honour rows unchecked after the bundle was chosen', () => {
            mockSelectedItems.set([
                contentlet({ inode: 'inode-1', identifier: 'id-1' }),
                contentlet({ inode: 'inode-2', identifier: 'id-2' })
            ]);
            goToBundleStep();
            chooseBundle();
            spectator.click('[data-testid="action-configure-continue"]');
            spectator.detectChanges();

            uncheckFirstRow();
            spectator.click('[data-testid="action-preview-execute"]');

            expect(store.executeAddToBundle).toHaveBeenCalledWith(expect.any(String), BUNDLE, [
                'id-2'
            ]);
        });

        it('should step back from the preview to the bundle step', () => {
            goToBundleStep();
            chooseBundle();
            spectator.click('[data-testid="action-configure-continue"]');
            spectator.detectChanges();

            spectator.click('[data-testid="action-preview-back"]');
            spectator.detectChanges();

            expect(spectator.query('[data-testid="action-configure-bundle-target"]')).toBeTruthy();
        });

        it('should drop the chosen bundle when returning to the action list', () => {
            goToBundleStep();
            chooseBundle();

            spectator.click('[data-testid="action-configure-back"]');
            spectator.detectChanges();

            expect(spectator.component['$selectedBundle']()).toBeNull();
        });

        it('should render the move step for a move action, not the bundle one', () => {
            // One `configure` view, two bodies: the discriminator has to pick the right one.
            spectator.detectChanges();
            spectator.component['$selectedActionId'].set('action-move');
            spectator.detectChanges();
            spectator.click('[data-testid="action-center-continue"]');
            spectator.detectChanges();

            expect(spectator.query('[data-testid="action-configure-move-target"]')).toBeTruthy();
            expect(spectator.query('[data-testid="action-configure-bundle-target"]')).toBeNull();
        });
    });

    describe('single-input workflow actions', () => {
        /** Arms an action and drills in, which stops on its configuration screen. */
        const goToConfigure = (actionId: string): void => {
            spectator.detectChanges();
            spectator.component['$selectedActionId'].set(actionId);
            spectator.detectChanges();
            spectator.click('[data-testid="action-center-continue"]');
            spectator.detectChanges();
        };

        describe('assign and comment', () => {
            it('should open the assign/comment screen, not the move or bundle one', () => {
                goToConfigure('action-assign');

                expect(
                    spectator.query('[data-testid="action-configure-assign-comment"]')
                ).toBeTruthy();
                expect(spectator.query('[data-testid="action-configure-move-target"]')).toBeNull();
                expect(spectator.query('[data-testid="action-preview"]')).toBeNull();
            });

            it('should pass the action assign metadata to the step', () => {
                // Asserted on the child's inputs rather than on `DotRolesService`: the step provides
                // that service itself, so a host-level mock is not the instance it calls. What is the
                // dialog's job — and what these two fields exist on the UI action shape for — is
                // handing the step the right role scope. The lookup itself is the step's own spec.
                goToConfigure('action-assign');

                const step = spectator.query(DotWorkflowAssignCommentComponent);

                expect(step?.roleId()).toBe('role-legal');
                expect(step?.roleHierarchy()).toBe(true);
                expect(step?.assignable()).toBe(true);
                expect(step?.commentable()).toBe(true);
            });

            it('should defer to the step for validity', () => {
                // Whether an assignee is required depends on roles only the step loaded.
                goToConfigure('action-assign');
                const continueButton = () =>
                    spectator.query(
                        '[data-testid="action-configure-continue"] button'
                    ) as HTMLButtonElement;

                spectator.component['$assignCommentValid'].set(false);
                spectator.detectChanges();
                expect(continueButton().disabled).toBe(true);

                spectator.component['$assignCommentValid'].set(true);
                spectator.detectChanges();
                expect(continueButton().disabled).toBe(false);
            });

            it('should fire with the assignee and comment collected', () => {
                goToConfigure('action-assign');
                spectator.component['onAssignCommentChange']({
                    assign: 'role-legal',
                    comment: 'Please review'
                });
                spectator.component['$assignCommentValid'].set(true);
                spectator.detectChanges();

                spectator.click('[data-testid="action-configure-continue"]');
                spectator.detectChanges();
                spectator.click('[data-testid="action-preview-execute"]');

                expect(store.executeWorkflowAction).toHaveBeenCalledWith(
                    'action-assign',
                    'Send to Legal',
                    ['inode-1', 'inode-2'],
                    expect.objectContaining({
                        assignComment: { assign: 'role-legal', comment: 'Please review' }
                    })
                );
            });
        });

        describe('push publish', () => {
            it('should open the push publish screen', () => {
                goToConfigure('action-pp');

                expect(
                    spectator.query('[data-testid="action-configure-push-publish"]')
                ).toBeTruthy();
                expect(spectator.query('[data-testid="action-preview"]')).toBeNull();
            });

            it('should defer to the step for validity', () => {
                goToConfigure('action-pp');
                const continueButton = () =>
                    spectator.query(
                        '[data-testid="action-configure-continue"] button'
                    ) as HTMLButtonElement;

                expect(continueButton().disabled).toBe(true);

                spectator.component['$pushPublishValid'].set(true);
                spectator.detectChanges();
                expect(continueButton().disabled).toBe(false);
            });

            it('should fire with the push publish settings collected', () => {
                const settings = {
                    whereToSend: 'env-1',
                    iWantTo: 'publish' as const,
                    publishDate: '2026-08-12',
                    publishTime: '10-30',
                    expireDate: '2026-08-12',
                    expireTime: '10-30',
                    filterKey: 'filter-b',
                    timezoneId: 'Europe/Madrid'
                };

                goToConfigure('action-pp');
                spectator.component['onPushPublishChange'](settings);
                spectator.component['$pushPublishValid'].set(true);
                spectator.detectChanges();

                spectator.click('[data-testid="action-configure-continue"]');
                spectator.detectChanges();
                spectator.click('[data-testid="action-preview-execute"]');

                expect(store.executeWorkflowAction).toHaveBeenCalledWith(
                    'action-pp',
                    'Push Publish',
                    ['inode-1', 'inode-2'],
                    expect.objectContaining({ pushPublish: settings })
                );
            });

            it('should refuse to fire before the step is satisfied', () => {
                // Reachable only by skipping the guarded Continue; a push with no environment has
                // nowhere to go, so the commit point refuses it too.
                goToConfigure('action-pp');
                spectator.component['$view'].set('preview');
                spectator.detectChanges();

                spectator.click('[data-testid="action-preview-execute"]');

                expect(store.executeWorkflowAction).not.toHaveBeenCalled();
            });
        });

        it('should drop collected settings when returning to the action list', () => {
            goToConfigure('action-pp');
            spectator.component['$pushPublishValid'].set(true);
            spectator.detectChanges();

            spectator.click('[data-testid="action-configure-back"]');
            spectator.detectChanges();

            expect(spectator.component['$pushPublish']()).toBeNull();
            expect(spectator.component['$pushPublishValid']()).toBe(false);
        });
    });

    describe('actions needing several configuration sections', () => {
        /** Arms `action-approve` (assignable + push publish) and drills into its configure screen. */
        const goToConfigure = (): void => {
            spectator.detectChanges();
            spectator.component['$selectedActionId'].set('action-approve');
            spectator.detectChanges();
            spectator.click('[data-testid="action-center-continue"]');
            spectator.detectChanges();
        };

        /** Satisfies both sections, as their step components would. */
        const satisfyBoth = (): void => {
            spectator.component['onAssignCommentChange']({ assign: 'role-1', comment: '' });
            spectator.component['$assignCommentValid'].set(true);
            spectator.component['onPushPublishChange'](PUSH_PUBLISH_SETTINGS);
            spectator.component['$pushPublishValid'].set(true);
            spectator.detectChanges();
        };

        const continueButton = (): HTMLButtonElement =>
            spectator.query(
                '[data-testid="action-configure-continue"] button'
            ) as HTMLButtonElement;

        it('should render every section the action needs on one screen', () => {
            goToConfigure();

            expect(spectator.query('[data-testid="action-configure-assign-comment"]')).toBeTruthy();
            expect(spectator.query('[data-testid="action-configure-push-publish"]')).toBeTruthy();
        });

        it('should render them in the legacy wizard order', () => {
            goToConfigure();

            const order = spectator
                .queryAll('[data-testid^="action-configure-section-"]')
                .map((section) => section.getAttribute('data-testid'));

            expect(order).toEqual([
                'action-configure-section-assignComment',
                'action-configure-section-pushPublish'
            ]);
        });

        it('should label the sections once there is more than one', () => {
            // A lone form is left unlabelled — the dialog header already names the action.
            goToConfigure();

            expect(spectator.queryAll('[data-testid^="action-configure-section-"] h3').length).toBe(
                2
            );
        });

        it('should keep Continue disabled until every section is satisfied', () => {
            goToConfigure();
            expect(continueButton().disabled).toBe(true);

            // One of two is not enough.
            spectator.component['$assignCommentValid'].set(true);
            spectator.detectChanges();
            expect(continueButton().disabled).toBe(true);

            spectator.component['$pushPublishValid'].set(true);
            spectator.detectChanges();
            expect(continueButton().disabled).toBe(false);
        });

        it('should name the first unsatisfied section in the footer', () => {
            // With sections stacked, the field holding Continue back can be scrolled out of view, so
            // the footer has to say which one it is.
            goToConfigure();
            expect(spectator.component['$configureHint']()).toBe(
                'content-drive.action-center.assign.no-assignee'
            );

            spectator.component['$assignCommentValid'].set(true);
            spectator.detectChanges();

            expect(spectator.component['$configureHint']()).toBe(
                'content-drive.action-center.push-publish.no-environment'
            );
        });

        it('should clear the hint once nothing is missing', () => {
            goToConfigure();
            satisfyBoth();

            expect(spectator.component['$configureHint']()).toBe('');
        });

        it('should fire one request carrying both payloads', () => {
            // The point of stacking: two sections, one execute. Both step components emit from an
            // effect, so this also guards against one emission clobbering the other.
            goToConfigure();
            satisfyBoth();

            spectator.click('[data-testid="action-configure-continue"]');
            spectator.detectChanges();
            spectator.click('[data-testid="action-preview-execute"]');

            expect(store.executeWorkflowAction).toHaveBeenCalledTimes(1);
            expect(store.executeWorkflowAction).toHaveBeenCalledWith(
                'action-approve',
                'Approve and Push',
                ['inode-1', 'inode-2'],
                {
                    pathToMove: '',
                    assignComment: { assign: 'role-1', comment: '' },
                    pushPublish: PUSH_PUBLISH_SETTINGS
                }
            );
        });

        it('should refuse to fire while a section is still unsatisfied', () => {
            goToConfigure();
            spectator.component['$assignCommentValid'].set(true);
            spectator.component['$view'].set('preview');
            spectator.detectChanges();

            spectator.click('[data-testid="action-preview-execute"]');

            expect(store.executeWorkflowAction).not.toHaveBeenCalled();
        });

        it('should use the generic back label rather than the move-specific one', () => {
            goToConfigure();

            expect(spectator.component['$backLabel']()).toBe(
                'content-drive.action-center.back.settings'
            );
        });
    });

    describe('surviving the trip to the preview and back', () => {
        /**
         * The regression guard for the two bugs found in review.
         *
         * Both had the same root cause: the `configure` block was destroyed on Continue, so stepping
         * back re-created each step component — and every one of them emits its own fresh state on
         * mount, overwriting what the dialog was holding rather than merely forgetting it. The step
         * sections stay mounted now, so nothing is re-created and nothing re-emits.
         */
        const armAndConfigure = (actionId: string): void => {
            spectator.detectChanges();
            spectator.component['$selectedActionId'].set(actionId);
            spectator.detectChanges();
            spectator.click('[data-testid="action-center-continue"]');
            spectator.detectChanges();
        };

        const goToPreviewAndBack = (): void => {
            spectator.click('[data-testid="action-configure-continue"]');
            spectator.detectChanges();
            spectator.click('[data-testid="action-preview-back"]');
            spectator.detectChanges();
        };

        it('should keep a chosen move destination', () => {
            armAndConfigure('action-move');
            spectator.component['onPathToMoveChange']('//demo.dotcms.com/application');
            spectator.detectChanges();

            goToPreviewAndBack();

            expect(spectator.component['$pathToMove']()).toBe('//demo.dotcms.com/application');
        });

        it('should not re-create the destination picker, which would re-seed and re-emit', () => {
            // The picker is a `ControlValueAccessor` that pushes a value outward as soon as its store
            // confirms a node. A re-created one would emit the *browsing* folder over the chosen
            // destination — and in the window before that resolves, Execute could fire a path the UI
            // never showed.
            armAndConfigure('action-move');
            const before = spectator.query('[data-testid="action-configure-move-target"]');

            goToPreviewAndBack();

            expect(spectator.query('[data-testid="action-configure-move-target"]')).toBe(before);
        });

        it('should keep a typed comment and chosen assignee', () => {
            armAndConfigure('action-assign');
            spectator.component['onAssignCommentChange']({
                assign: 'role-legal',
                comment: 'Please review'
            });
            spectator.component['$assignCommentValid'].set(true);
            spectator.detectChanges();

            goToPreviewAndBack();

            expect(spectator.component['$assignComment']()).toEqual({
                assign: 'role-legal',
                comment: 'Please review'
            });
        });

        it('should keep push publish settings', () => {
            armAndConfigure('action-pp');
            spectator.component['onPushPublishChange'](PUSH_PUBLISH_SETTINGS);
            spectator.component['$pushPublishValid'].set(true);
            spectator.detectChanges();

            goToPreviewAndBack();

            expect(spectator.component['$pushPublish']()).toEqual(PUSH_PUBLISH_SETTINGS);
        });

        it('should hide the sections behind the preview rather than removing them', () => {
            armAndConfigure('action-pp');
            spectator.component['$pushPublishValid'].set(true);
            spectator.detectChanges();

            spectator.click('[data-testid="action-configure-continue"]');
            spectator.detectChanges();

            const body = spectator.query('[data-testid="action-configure-body"]');

            expect(body).toBeTruthy();
            expect(body?.classList.contains('hidden')).toBe(true);
            expect(spectator.query('[data-testid="action-preview"]')).toBeTruthy();
        });

        it('should still discard everything when returning to the action list', () => {
            // Back to actions is a real reset — a different action must not inherit the last one's
            // answers.
            armAndConfigure('action-pp');
            spectator.component['onPushPublishChange'](PUSH_PUBLISH_SETTINGS);
            spectator.detectChanges();

            spectator.click('[data-testid="action-configure-back"]');
            spectator.detectChanges();

            expect(spectator.component['$pushPublish']()).toBeNull();
        });
    });

    describe('the action list footer', () => {
        it('should offer one Continue for the whole screen, not one per scheme', () => {
            // A per-panel button repeated a single global decision — only one action can be armed
            // across every scheme — and sat below a scrolling list, so the radio and the button
            // acting on it could not be seen at once.
            spectator.detectChanges();

            expect(spectator.queryAll('[data-testid^="continue-workflow-"]')).toEqual([]);
            expect(spectator.query('[data-testid="action-center-continue"]')).toBeTruthy();
        });

        it('should arm the footer Continue from any scheme panel', () => {
            armAction();

            const continueButton = spectator.query(
                '[data-testid="action-center-continue"] button'
            ) as HTMLButtonElement;

            expect(continueButton.disabled).toBe(false);
        });

        it('should no longer close the dialog from the action list', () => {
            // Done is gone: the screen's one button now advances instead of dismissing. Closing is
            // the shell's job, through the X, ESC and the mask.
            spectator.detectChanges();

            expect(spectator.query('[data-testid="action-center-done"]')).toBeNull();
            expect(store.closeDialog).not.toHaveBeenCalled();
        });
    });
});
