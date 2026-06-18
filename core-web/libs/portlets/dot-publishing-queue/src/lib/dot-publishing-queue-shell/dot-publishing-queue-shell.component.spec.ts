import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Subject, of } from 'rxjs';

import { CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

/* eslint-disable @nx/enforce-module-boundaries */

import {
    DotCurrentUserService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotPublishingQueueService
} from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';

import { DotPublishingQueueShellComponent } from './dot-publishing-queue-shell.component';

import { DotPublishingQueueStore } from '../dot-publishing-queue-page/store/dot-publishing-queue.store';

describe('DotPublishingQueueShellComponent', () => {
    let spectator: Spectator<DotPublishingQueueShellComponent>;
    let dialogService: jest.Mocked<DialogService>;
    let confirmationService: jest.Mocked<ConfirmationService>;
    let store: InstanceType<typeof DotPublishingQueueStore>;

    let onCloseSubject = new Subject<unknown>();
    const dialogRef = {
        close: jest.fn(),
        get onClose() {
            return onCloseSubject;
        }
    } as unknown as DynamicDialogRef;

    const createComponent = createComponentFactory({
        component: DotPublishingQueueShellComponent,
        componentProviders: [
            DotPublishingQueueStore,
            ConfirmationService,
            mockProvider(DialogService, { open: jest.fn().mockReturnValue(dialogRef) })
        ],
        providers: [
            mockProvider(DotPublishingQueueService, {
                listPublishingJobs: jest.fn().mockReturnValue(
                    of({
                        entity: [],
                        pagination: { currentPage: 1, perPage: 10, totalEntries: 0 }
                    })
                ),
                getUnsendBundles: jest
                    .fn()
                    .mockReturnValue(
                        of({ identifier: 'id', label: 'name', items: [], numRows: 0 })
                    ),
                getBundleAssets: jest.fn().mockReturnValue(of([])),
                getPublishingJobDetails: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest.fn().mockReturnValue(of({ userId: 'dotcms.org.1' }))
            }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotDownloadBundleDialogService, { open: jest.fn() }),
            mockProvider(DotGlobalMessageService, { error: jest.fn() }),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA]
    });

    beforeEach(() => {
        jest.clearAllMocks();
        onCloseSubject = new Subject<unknown>();
        spectator = createComponent();
        dialogService = spectator.inject(DialogService, true) as jest.Mocked<DialogService>;
        confirmationService = spectator.inject(
            ConfirmationService,
            true
        ) as jest.Mocked<ConfirmationService>;
        jest.spyOn(confirmationService, 'confirm');
        store = spectator.inject(DotPublishingQueueStore, true);
    });

    it('renders the toolbar', () => {
        expect(spectator.query('dot-publishing-queue-toolbar')).toBeTruthy();
    });

    describe('asset list dialog sync', () => {
        it('opens dialog when selectedBundleId becomes set', () => {
            store.openAssetList('B-1');
            spectator.detectChanges();
            expect(dialogService.open).toHaveBeenCalled();
        });

        it('calls store.closeAssetList when dialog closes', () => {
            store.openAssetList('B-1');
            spectator.detectChanges();
            onCloseSubject.next(undefined);
            expect(store.selectedBundleId()).toBeNull();
        });
    });

    describe('detail dialog sync', () => {
        it('opens dialog when detailBundleId becomes set', () => {
            store.openDetail('B-2');
            spectator.detectChanges();
            expect(dialogService.open).toHaveBeenCalled();
        });
    });

    describe('upload', () => {
        it('opens dialog when openUpload is called', () => {
            spectator.component.openUpload();
            expect(dialogService.open).toHaveBeenCalled();
        });
    });

    describe('tab change', () => {
        it('forwards value to setActiveTab', () => {
            spectator.component.onTabChange('history');
            expect(store.activeTab()).toBe('history');
            spectator.component.onTabChange('queue');
            expect(store.activeTab()).toBe('queue');
        });
    });

    describe('delete bundles dialog', () => {
        function openAndCloseWith(scope: 'selected' | 'all' | 'success' | 'failed' | undefined) {
            spectator.component.openDeleteBundles();
            expect(dialogService.open).toHaveBeenCalled();
            onCloseSubject.next(scope);
        }

        it('opens the delete dialog when openDeleteBundles is called', () => {
            spectator.component.openDeleteBundles();
            expect(dialogService.open).toHaveBeenCalled();
        });

        it('does nothing when the dialog closes with no scope (X / ESC / overlay)', () => {
            jest.spyOn(store, 'deleteBundlesBulk');
            jest.spyOn(store, 'purgeBundles');
            openAndCloseWith(undefined);
            expect(store.deleteBundlesBulk).not.toHaveBeenCalled();
            expect(store.purgeBundles).not.toHaveBeenCalled();
            expect(confirmationService.confirm).not.toHaveBeenCalled();
        });

        it('SELECTED → store.deleteBundlesBulk with current selected ids', () => {
            store.setHistorySelection(['b1', 'b2']);
            const spy = jest.spyOn(store, 'deleteBundlesBulk').mockReturnValue(undefined);
            openAndCloseWith('selected');
            expect(spy).toHaveBeenCalledWith(['b1', 'b2']);
        });

        it('SUCCESS → store.purgeBundles with the SUCCESS status list', () => {
            const spy = jest.spyOn(store, 'purgeBundles').mockReturnValue(undefined);
            openAndCloseWith('success');
            expect(spy).toHaveBeenCalled();
            const statuses = spy.mock.calls[0][0] as readonly string[];
            expect(statuses).toEqual(expect.arrayContaining(['SUCCESS', 'SUCCESS_WITH_WARNINGS']));
        });

        it('FAILED → store.purgeBundles with the legacy 5-status FAILED list', () => {
            const spy = jest.spyOn(store, 'purgeBundles').mockReturnValue(undefined);
            openAndCloseWith('failed');
            expect(spy).toHaveBeenCalled();
            const statuses = spy.mock.calls[0][0] as readonly string[];
            expect(statuses).toEqual(
                expect.arrayContaining([
                    'FAILED_TO_SEND_TO_ALL_GROUPS',
                    'FAILED_TO_SEND_TO_SOME_GROUPS',
                    'FAILED_TO_BUNDLE',
                    'FAILED_TO_SENT',
                    'FAILED_TO_PUBLISH'
                ])
            );
            // Must NOT include the 3 newer statuses (per legacy /api/bundle/all/fail)
            expect(statuses).toEqual(
                expect.not.arrayContaining([
                    'FAILED_INTEGRITY_CHECK',
                    'INVALID_TOKEN',
                    'LICENSE_REQUIRED'
                ])
            );
        });

        it('ALL → confirmation dialog; purgeBundles() with no statuses on accept', () => {
            const purgeSpy = jest.spyOn(store, 'purgeBundles').mockReturnValue(undefined);
            confirmationService.confirm.mockImplementation((cfg) => {
                cfg.accept?.();
                return confirmationService;
            });
            openAndCloseWith('all');
            expect(confirmationService.confirm).toHaveBeenCalled();
            expect(purgeSpy).toHaveBeenCalledWith();
        });

        it('ALL → no purge if the user rejects the confirmation', () => {
            const purgeSpy = jest.spyOn(store, 'purgeBundles').mockReturnValue(undefined);
            confirmationService.confirm.mockImplementation((cfg) => {
                cfg.reject?.();
                return confirmationService;
            });
            openAndCloseWith('all');
            expect(purgeSpy).not.toHaveBeenCalled();
        });
    });
});
