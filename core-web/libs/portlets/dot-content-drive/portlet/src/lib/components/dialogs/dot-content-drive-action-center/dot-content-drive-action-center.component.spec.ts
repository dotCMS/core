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
                closeDialog: jest.fn()
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
        jest.spyOn(fireService, 'fireDefaultAction').mockReturnValue(of([]));
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

            expect(fireService.fireDefaultAction).not.toHaveBeenCalled();
        });

        it('should confirm before firing Delete, then fire on accept', () => {
            // Delete only applies to archived items, and only it carries a confirmMessage.
            mockSelectedItems.set([contentlet({ inode: 'inode-1', archived: true })]);
            jest.spyOn(confirmationService, 'confirm').mockImplementation((config) => {
                config.accept?.();

                return confirmationService;
            });

            spectator.detectChanges();
            spectator.click('[data-testid="quick-action-DELETE"]');

            expect(confirmationService.confirm).toHaveBeenCalled();
            expect(fireService.fireDefaultAction).toHaveBeenCalledWith({
                action: 'DELETE',
                inodes: ['inode-1']
            });
        });

        it('should not fire Delete when the confirmation is dismissed', () => {
            mockSelectedItems.set([contentlet({ inode: 'inode-1', archived: true })]);
            // Default mock records the call without invoking `accept`.
            spectator.detectChanges();

            spectator.click('[data-testid="quick-action-DELETE"]');

            expect(confirmationService.confirm).toHaveBeenCalled();
            expect(fireService.fireDefaultAction).not.toHaveBeenCalled();
        });

        it('should fire non-destructive actions without confirming', () => {
            spectator.detectChanges();

            spectator.click('[data-testid="quick-action-PUBLISH"]');

            expect(confirmationService.confirm).not.toHaveBeenCalled();
            expect(fireService.fireDefaultAction).toHaveBeenCalled();
        });

        it('should not fire an action that applies to nothing', () => {
            spectator.detectChanges();

            spectator.click('[data-testid="quick-action-DELETE"]');

            expect(fireService.fireDefaultAction).not.toHaveBeenCalled();
        });

        it('should fire only the inodes the action applies to, not the whole selection', () => {
            // Selection is inode-1 (not live) and inode-2 (live). Publish applies to inode-1 only.
            spectator.detectChanges();

            spectator.click('[data-testid="quick-action-PUBLISH"]');

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

            spectator.click('[data-testid="quick-action-PUBLISH"]');

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

            spectator.detectChanges();
            spectator.click('[data-testid="quick-action-DELETE"]');

            expect(fireService.fireDefaultAction).toHaveBeenCalledWith({
                action: 'DELETE',
                inodes: ['archived-1']
            });
        });

        it('should refresh the grid and close the dialog on success', () => {
            spectator.detectChanges();

            spectator.click('[data-testid="quick-action-PUBLISH"]');

            expect(store.loadItems).toHaveBeenCalled();
            expect(store.closeDialog).toHaveBeenCalled();
        });

        it('should hand errors to the http error manager without closing the dialog', () => {
            const error = new HttpErrorResponse({ status: 403 });
            jest.spyOn(fireService, 'fireDefaultAction').mockReturnValue(throwError(() => error));

            spectator.detectChanges();
            spectator.click('[data-testid="quick-action-PUBLISH"]');

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.closeDialog).not.toHaveBeenCalled();
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
    });

    describe('workflow action preview', () => {
        beforeEach(() => goToPreview());

        it('should list every selected contentlet, all included', () => {
            expect(spectator.queryAll('[data-testid="preview-row"]').length).toBe(2);
            expect(spectator.component['$includedCount']()).toBe(2);
        });

        it('should title the preview with the action name and show the included count', () => {
            expect(spectator.query('[data-testid="action-preview-title"]').textContent).toContain(
                'Send for Review'
            );
            expect(spectator.query('[data-testid="action-preview-count"]').textContent).toContain(
                'content-drive.action-center.items-selected'
            );
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
