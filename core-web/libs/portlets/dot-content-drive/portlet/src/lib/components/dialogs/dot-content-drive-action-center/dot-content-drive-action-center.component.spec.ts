import { afterEach, beforeEach, describe, expect, it, jest } from '@jest/globals';
import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';

import { ConfirmationService, MessageService } from 'primeng/api';

import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotBulkActionView, DotContentDriveItem } from '@dotcms/dotcms-models';

import { DotContentDriveActionCenterComponent } from './dot-content-drive-action-center.component';

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
                        }
                    ]
                }
            ]
        }
    ]
} as DotBulkActionView;

describe('DotContentDriveActionCenterComponent', () => {
    let spectator: Spectator<DotContentDriveActionCenterComponent>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;
    let messageService: SpyObject<MessageService>;
    let workflowsActionsService: SpyObject<DotWorkflowsActionsService>;
    let fireService: SpyObject<DotWorkflowActionsFireService>;
    let confirmationService: SpyObject<ConfirmationService>;
    let httpErrorManager: SpyObject<DotHttpErrorManagerService>;

    const mockSelectedItems = signal<DotContentDriveItem[]>([]);

    const createComponent = createComponentFactory({
        component: DotContentDriveActionCenterComponent,
        providers: [
            provideHttpClient(),
            mockProvider(DotContentDriveStore, {
                selectedItems: mockSelectedItems,
                loadItems: jest.fn(),
                setStatus: jest.fn(),
                setSelectedItems: jest.fn(),
                closeDialog: jest.fn(),
                setDialogDrillDown: jest.fn(),
                clearDialogDrillDown: jest.fn()
            }),
            mockProvider(MessageService, { add: jest.fn() }),
            mockProvider(DotMessageService, {
                get: jest.fn().mockImplementation((key: string) => key)
            }),
            mockProvider(DotHttpErrorManagerService, {
                handle: jest.fn()
            })
        ],
        detectChanges: false
    });

    beforeEach(() => {
        mockSelectedItems.set([
            contentlet({ inode: 'inode-1' }),
            contentlet({ inode: 'inode-2', live: true })
        ]);

        spectator = createComponent();

        store = spectator.inject(DotContentDriveStore, true);
        messageService = spectator.inject(MessageService, true);
        workflowsActionsService = spectator.inject(DotWorkflowsActionsService, true);
        fireService = spectator.inject(DotWorkflowActionsFireService, true);
        confirmationService = spectator.inject(ConfirmationService, true);
        httpErrorManager = spectator.inject(DotHttpErrorManagerService);

        jest.spyOn(workflowsActionsService, 'getBulkActions').mockReturnValue(
            of(BULK_ACTIONS_RESPONSE)
        );
        jest.spyOn(fireService, 'fireDefaultAction').mockReturnValue(
            of({
                results: [],
                summary: { affected: 1, successCount: 1, failCount: 0, time: 1 }
            })
        );
        jest.spyOn(fireService, 'bulkFire').mockReturnValue(
            of({ successCount: 2, skippedCount: 0, fails: [] })
        );
        jest.spyOn(store, 'closeDialog');
        jest.spyOn(store, 'loadItems');
        jest.spyOn(messageService, 'add');
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
        spectator.click('[data-testid="continue-workflow-editorial"]');
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

    /** Clicks the real checkbox of the first preview row, dropping it from the included set. */
    const uncheckFirstRow = (): void => {
        const checkbox = spectator
            .queryAll('[data-testid="preview-row"]')[0]
            .querySelector('[data-testid="preview-row-checkbox"] input');

        spectator.click(checkbox);
        spectator.detectChanges();
    };

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
    });

    describe('quick actions', () => {
        it('should render a quick action for the eligible subset', () => {
            spectator.detectChanges();

            // inode-1 is not live, so Publish applies to exactly one item.
            expect(spectator.query('[data-testid="quick-action-PUBLISH"]')).toBeTruthy();
        });

        it('should render actions that apply to nothing as non-selectable', () => {
            // Nothing archived, so Delete applies to no item — the row stays, disabled.
            spectator.detectChanges();

            const remove = spectator.query(
                '[data-testid="quick-action-DELETE"]'
            ) as HTMLButtonElement;

            expect(remove).toBeTruthy();
            expect(remove.disabled).toBe(true);
        });

        it('should keep applicable actions selectable', () => {
            spectator.detectChanges();

            const publish = spectator.query(
                '[data-testid="quick-action-PUBLISH"]'
            ) as HTMLButtonElement;

            expect(publish.disabled).toBe(false);
        });

        it('should render Add to Bundle but keep it non-selectable', () => {
            spectator.detectChanges();

            const addToBundle = spectator.query(
                '[data-testid="quick-action-ADD_TO_BUNDLE"]'
            ) as HTMLButtonElement;

            expect(addToBundle).toBeTruthy();
            expect(addToBundle.disabled).toBe(true);
        });

        it('should not fire Add to Bundle even if its row is clicked', () => {
            spectator.detectChanges();

            spectator.click('[data-testid="quick-action-ADD_TO_BUNDLE"]');
            spectator.detectChanges();

            expect(spectator.query('[data-testid="action-preview"]')).toBeNull();
            expect(fireService.fireDefaultAction).not.toHaveBeenCalled();
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
            expect(fireService.fireDefaultAction).toHaveBeenCalledWith({
                action: 'DELETE',
                inodes: ['inode-1']
            });
        });

        it('should not fire Delete when the confirmation is dismissed', () => {
            mockSelectedItems.set([contentlet({ inode: 'inode-1', archived: true })]);
            // Default mock records the call without invoking `accept`.
            executeQuickAction('DELETE');

            expect(confirmationService.confirm).toHaveBeenCalled();
            expect(fireService.fireDefaultAction).not.toHaveBeenCalled();
        });

        it('should fire non-destructive actions without confirming', () => {
            executeQuickAction('PUBLISH');

            expect(confirmationService.confirm).not.toHaveBeenCalled();
            expect(fireService.fireDefaultAction).toHaveBeenCalled();
        });

        it('should not fire an action that applies to nothing', () => {
            spectator.detectChanges();

            spectator.click('[data-testid="quick-action-DELETE"]');
            spectator.detectChanges();

            // Never even reaches the preview.
            expect(spectator.query('[data-testid="action-preview"]')).toBeNull();
            expect(fireService.fireDefaultAction).not.toHaveBeenCalled();
        });

        it('should fire only the inodes the action applies to, not the whole selection', () => {
            // Selection is inode-1 (not live) and inode-2 (live). Publish applies to inode-1 only.
            executeQuickAction('PUBLISH');

            expect(fireService.fireDefaultAction).toHaveBeenCalledWith({
                action: 'PUBLISH',
                inodes: ['inode-1']
            });
        });

        it('should fire exactly as many inodes as the row advertises', () => {
            // Guards the count/payload pair against drifting apart again: whatever number the row
            // shows must equal the number of inodes sent.
            spectator.detectChanges();

            const row = spectator.query('[data-testid="quick-action-PUBLISH"]');
            const advertised = Number(row?.textContent?.match(/\((\d+)\)/)?.[1]);

            executeQuickAction('PUBLISH');

            const fired = (fireService.fireDefaultAction as unknown as jest.Mock).mock
                .calls[0][0] as { inodes: string[] };

            expect(advertised).toBe(1);
            expect(fired.inodes).toHaveLength(advertised);
        });

        it('should fire only archived items for Delete', () => {
            mockSelectedItems.set([
                contentlet({ inode: 'archived-1', archived: true }),
                contentlet({ inode: 'live-1', live: true })
            ]);
            jest.spyOn(confirmationService, 'confirm').mockImplementation((config) => {
                config.accept?.();

                return confirmationService;
            });

            executeQuickAction('DELETE');

            expect(fireService.fireDefaultAction).toHaveBeenCalledWith({
                action: 'DELETE',
                inodes: ['archived-1']
            });
        });

        it('should refresh the grid and close the dialog on success', () => {
            executeQuickAction('PUBLISH');

            expect(store.loadItems).toHaveBeenCalled();
            expect(store.closeDialog).toHaveBeenCalled();
        });

        it('should hand errors to the http error manager without closing the dialog', () => {
            const error = new HttpErrorResponse({ status: 403 });
            jest.spyOn(fireService, 'fireDefaultAction').mockReturnValue(throwError(() => error));

            executeQuickAction('PUBLISH');

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.closeDialog).not.toHaveBeenCalled();
        });

        it('should report the counts the endpoint returned, not the number of inodes sent', () => {
            // Per-item failures are an expected outcome (a lock held by another user, a permission
            // the row state can't see), so the toast has to reflect what the server actually did.
            jest.spyOn(fireService, 'fireDefaultAction').mockReturnValue(
                of({
                    results: [],
                    summary: { affected: 2, successCount: 1, failCount: 1, time: 1 }
                })
            );

            executeQuickAction('PUBLISH');

            expect(messageService.add).toHaveBeenCalledWith(
                expect.objectContaining({
                    severity: 'warn',
                    detail: expect.stringContaining('executed-with-fails')
                })
            );
        });

        it('should report a plain success when nothing failed', () => {
            executeQuickAction('PUBLISH');

            expect(messageService.add).toHaveBeenCalledWith(
                expect.objectContaining({
                    severity: 'success',
                    detail: expect.stringContaining('executed-detail')
                })
            );
        });
    });

    describe('lock and unlock quick actions', () => {
        it('should fire LOCK with only the unlocked inodes', () => {
            mockSelectedItems.set([
                contentlet({ inode: 'unlocked-1', locked: false }),
                contentlet({ inode: 'locked-1', locked: true })
            ]);

            executeQuickAction('LOCK');

            expect(fireService.fireDefaultAction).toHaveBeenCalledWith({
                action: 'LOCK',
                inodes: ['unlocked-1']
            });
        });

        it('should fire UNLOCK with only the locked inodes', () => {
            mockSelectedItems.set([
                contentlet({ inode: 'unlocked-1', locked: false }),
                contentlet({ inode: 'locked-1', locked: true })
            ]);

            executeQuickAction('UNLOCK');

            expect(fireService.fireDefaultAction).toHaveBeenCalledWith({
                action: 'UNLOCK',
                inodes: ['locked-1']
            });
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

        it('should still fire every locked item when some are held by other users', () => {
            // Attempt-all is deliberate: the client cannot know whether the user holds the CMS
            // Administrator role that lets them release someone else's lock.
            mockSelectedItems.set([
                contentlet({ inode: 'mine', locked: true, contentEditable: true }),
                contentlet({ inode: 'theirs', locked: true, contentEditable: false })
            ]);

            executeQuickAction('UNLOCK');

            expect(fireService.fireDefaultAction).toHaveBeenCalledWith({
                action: 'UNLOCK',
                inodes: ['mine', 'theirs']
            });
        });

        it('should report partially failed unlocks back to the user', () => {
            jest.spyOn(fireService, 'fireDefaultAction').mockReturnValue(
                of({
                    results: [],
                    summary: { affected: 2, successCount: 1, failCount: 1, time: 1 }
                })
            );
            mockSelectedItems.set([
                contentlet({ inode: 'mine', locked: true, contentEditable: true }),
                contentlet({ inode: 'theirs', locked: true, contentEditable: false })
            ]);

            executeQuickAction('UNLOCK');

            expect(messageService.add).toHaveBeenCalledWith(
                expect.objectContaining({
                    severity: 'warn',
                    detail: expect.stringContaining('executed-with-fails')
                })
            );
        });

        it('should exclude folders from Lock', () => {
            mockSelectedItems.set([
                contentlet({ inode: 'unlocked-1', locked: false }),
                folder('folder-1')
            ]);

            executeQuickAction('LOCK');

            expect(fireService.fireDefaultAction).toHaveBeenCalledWith({
                action: 'LOCK',
                inodes: ['unlocked-1']
            });
        });

        it('should refresh the grid and clear the selection after unlocking', () => {
            mockSelectedItems.set([contentlet({ inode: 'locked-1', locked: true })]);

            executeQuickAction('UNLOCK');

            expect(store.setSelectedItems).toHaveBeenCalledWith([]);
            expect(store.loadItems).toHaveBeenCalled();
            expect(store.closeDialog).toHaveBeenCalled();
        });

        it('should not confirm before locking or unlocking', () => {
            mockSelectedItems.set([contentlet({ inode: 'locked-1', locked: true })]);

            executeQuickAction('UNLOCK');

            expect(confirmationService.confirm).not.toHaveBeenCalled();
        });
    });

    describe('quick action preview', () => {
        it('should open the preview instead of firing when a quick action is clicked', () => {
            openQuickActionPreview('PUBLISH');

            expect(spectator.query('[data-testid="action-preview"]')).toBeTruthy();
            expect(spectator.query('[data-testid="action-center"]')).toBeNull();
            expect(fireService.fireDefaultAction).not.toHaveBeenCalled();
        });

        it('should list only the contentlets the quick action applies to', () => {
            // inode-1 is not live, inode-2 is. Publish applies to inode-1 only, so the preview must
            // not offer inode-2 as something the user could include.
            openQuickActionPreview('PUBLISH');

            const rows = spectator.queryAll('[data-testid="preview-row"]');

            expect(rows).toHaveLength(1);
            expect(rows[0].textContent).toContain('Title inode-1');
        });

        it('should retitle the dialog header with the quick action and its count', () => {
            openQuickActionPreview('PUBLISH');

            expect(store.setDialogDrillDown).toHaveBeenCalledWith({
                header: 'Default-Action-Publish',
                itemCount: 1
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

            expect(fireService.fireDefaultAction).toHaveBeenCalledWith({
                action: 'PUBLISH',
                inodes: ['drop-me']
            });
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
            expect(fireService.fireDefaultAction).not.toHaveBeenCalled();
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

            expect(fireService.fireDefaultAction).toHaveBeenCalledWith({
                action: 'UNLOCK',
                inodes: ['theirs']
            });
        });
    });

    describe('workflow actions', () => {
        it('should keep Continue disabled until an action is selected', () => {
            spectator.detectChanges();

            // PrimeNG puts `disabled` on the inner <button>, not on the p-button host.
            const continueButton = spectator.query(
                '[data-testid="continue-workflow-editorial"] button'
            ) as HTMLButtonElement;

            expect(continueButton.disabled).toBe(true);
        });

        it('should open the preview instead of firing when Continue is clicked', () => {
            armAction();

            spectator.click('[data-testid="continue-workflow-editorial"]');
            spectator.detectChanges();

            expect(spectator.query('[data-testid="action-preview"]')).toBeTruthy();
            expect(spectator.query('[data-testid="action-center"]')).toBeNull();
            expect(fireService.bulkFire).not.toHaveBeenCalled();
        });

        it('should not fire when no action is selected', () => {
            spectator.detectChanges();

            spectator.component['onExecuteWorkflowAction']();

            expect(fireService.bulkFire).not.toHaveBeenCalled();
        });

        it('should not open the preview when no action is selected', () => {
            spectator.detectChanges();

            spectator.component['onContinueToPreview']();
            spectator.detectChanges();

            expect(spectator.query('[data-testid="action-preview"]')).toBeNull();
        });

        it('should disable actions that need extra input', () => {
            spectator.detectChanges();

            const pushPublish = spectator.query(
                '[data-testid="workflow-action-action-pp"] input'
            ) as HTMLInputElement;

            expect(pushPublish.disabled).toBe(true);
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
                    '[data-testid="continue-workflow-editorial"] button'
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
            expect(spectator.queryAll('[data-testid="preview-row"]').length).toBe(2);
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

            expect(fireService.bulkFire).toHaveBeenCalledWith(
                expect.objectContaining({
                    workflowActionId: 'action-review',
                    contentletIds: ['inode-1', 'inode-2']
                })
            );
        });

        it('should fire only the contentlets still checked', () => {
            // The whole point of the preview: unchecking a row must remove exactly that inode from
            // the payload. Before this screen existed the fire always sent the full selection.
            uncheckFirstRow();

            spectator.click('[data-testid="action-preview-execute"]');

            expect(fireService.bulkFire).toHaveBeenCalledWith(
                expect.objectContaining({ contentletIds: ['inode-2'] })
            );
        });

        it('should keep the fired payload in step with the count shown on the button', () => {
            uncheckFirstRow();

            const badge = spectator.query('[data-testid="action-preview-execute"] .p-badge');
            spectator.click('[data-testid="action-preview-execute"]');

            const [request] = (fireService.bulkFire as jest.Mock).mock.calls[0] as [
                { contentletIds: string[] }
            ];

            expect(request.contentletIds.length).toBe(Number(badge.textContent.trim()));
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

            expect(fireService.bulkFire).not.toHaveBeenCalled();
        });

        it('should surface skipped items in the result message', () => {
            jest.spyOn(fireService, 'bulkFire').mockReturnValue(
                of({ successCount: 1, skippedCount: 1, fails: [] })
            );

            spectator.click('[data-testid="action-preview-execute"]');

            expect(messageService.add).toHaveBeenCalledWith(
                expect.objectContaining({
                    detail: 'content-drive.action-center.toast.executed-with-skips'
                })
            );
        });

        it('should refresh the grid and close the dialog on success', () => {
            spectator.click('[data-testid="action-preview-execute"]');

            expect(store.loadItems).toHaveBeenCalled();
            expect(store.closeDialog).toHaveBeenCalled();
        });

        it('should keep the dialog open and report the error on failure', () => {
            jest.spyOn(fireService, 'bulkFire').mockReturnValue(
                throwError(() => new HttpErrorResponse({ status: 500 }))
            );

            spectator.click('[data-testid="action-preview-execute"]');

            expect(httpErrorManager.handle).toHaveBeenCalled();
            expect(store.closeDialog).not.toHaveBeenCalled();
        });

        describe('back', () => {
            it('should return to the actions view with the action still armed', () => {
                spectator.click('[data-testid="action-preview-back"]');
                spectator.detectChanges();

                expect(spectator.query('[data-testid="action-center"]')).toBeTruthy();
                expect(spectator.query('[data-testid="action-preview"]')).toBeNull();
                // Kept on purpose: re-entering the preview must not mean re-picking the action.
                expect(spectator.component['$selectedActionId']()).toBe('action-review');
                expect(fireService.bulkFire).not.toHaveBeenCalled();
            });

            it('should be inert while an action is in flight', () => {
                spectator.component['$executing'].set(true);
                spectator.detectChanges();

                spectator.component['onBackToActions']();
                spectator.detectChanges();

                expect(spectator.query('[data-testid="action-preview"]')).toBeTruthy();
            });
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
            spectator.click('[data-testid="continue-workflow-Blogs"]');
            spectator.detectChanges();

            const rows = spectator.queryAll('[data-testid="preview-row"]');

            expect(rows.length).toBe(1);
            expect(rows[0].getAttribute('data-inode')).toBe('blog-1');
        });

        it('should fire only the eligible contentlet', () => {
            spectator.detectChanges();
            spectator.component['$selectedActionId'].set('copy-blog');
            spectator.detectChanges();
            spectator.click('[data-testid="continue-workflow-Blogs"]');
            spectator.detectChanges();

            spectator.click('[data-testid="action-preview-execute"]');

            expect(fireService.bulkFire).toHaveBeenCalledWith(
                expect.objectContaining({ contentletIds: ['blog-1'] })
            );
        });

        it('should count the header against the eligible contentlets only', () => {
            spectator.detectChanges();
            spectator.component['$selectedActionId'].set('copy-blog');
            spectator.detectChanges();
            spectator.click('[data-testid="continue-workflow-Blogs"]');
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
            spectator.click('[data-testid="continue-workflow-Blogs"]');
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

    describe('done', () => {
        it('should close the dialog without firing anything', () => {
            spectator.detectChanges();

            spectator.click('[data-testid="action-center-done"]');

            expect(store.closeDialog).toHaveBeenCalled();
            expect(fireService.bulkFire).not.toHaveBeenCalled();
            expect(fireService.fireDefaultAction).not.toHaveBeenCalled();
        });
    });
});
