import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Subject, of } from 'rxjs';

import { CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

/* eslint-disable @nx/enforce-module-boundaries */

import {
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotPublishingQueueService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';

import { DotPublishingQueueShellComponent } from './dot-publishing-queue-shell.component';

import { DotPublishingQueueStore } from '../store/dot-publishing-queue.store';

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
                getBundleAssets: jest.fn().mockReturnValue(of([])),
                getPublishingJobDetails: jest.fn().mockReturnValue(of({})),
                probeBundleDownload: jest.fn().mockReturnValue(of(true)),
                probeBundleManifest: jest.fn().mockReturnValue(of(true))
            }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotGlobalMessageService, { error: jest.fn() }),
            mockProvider(DotMessageDisplayService, { push: jest.fn() }),
            mockProvider(DotPushPublishDialogService, { open: jest.fn() }),
            mockProvider(DotDownloadBundleDialogService, { open: jest.fn() }),
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

    it('renders the single bundles table (no tabs)', () => {
        expect(spectator.query('dot-publishing-queue-table')).toBeTruthy();
        expect(spectator.query('p-tabs')).toBeFalsy();
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

    describe('confirmDeleteBundles', () => {
        it('does nothing when there is no selection (defensive guard)', () => {
            jest.spyOn(store, 'deleteBundlesBulk');
            spectator.component.confirmDeleteBundles();
            expect(confirmationService.confirm).not.toHaveBeenCalled();
            expect(store.deleteBundlesBulk).not.toHaveBeenCalled();
        });

        it('opens a ConfirmDialog when there is a selection', () => {
            store.setBundlesSelection(['b1', 'b2']);
            spectator.component.confirmDeleteBundles();
            expect(confirmationService.confirm).toHaveBeenCalled();
        });

        it('calls store.deleteBundlesBulk with the selected ids on accept', () => {
            store.setBundlesSelection(['b1', 'b2']);
            const spy = jest.spyOn(store, 'deleteBundlesBulk').mockReturnValue(undefined);
            confirmationService.confirm.mockImplementation((cfg) => {
                cfg.accept?.();
                return confirmationService;
            });
            spectator.component.confirmDeleteBundles();
            expect(spy).toHaveBeenCalledWith(['b1', 'b2']);
        });

        it('does NOT delete on reject', () => {
            store.setBundlesSelection(['b1']);
            const spy = jest.spyOn(store, 'deleteBundlesBulk').mockReturnValue(undefined);
            confirmationService.confirm.mockImplementation((cfg) => {
                cfg.reject?.();
                return confirmationService;
            });
            spectator.component.confirmDeleteBundles();
            expect(spy).not.toHaveBeenCalled();
        });
    });
});
