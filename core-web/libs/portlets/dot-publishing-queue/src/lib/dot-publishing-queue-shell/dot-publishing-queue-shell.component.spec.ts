import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/vitest';
import { Subject, of } from 'rxjs';
import { Mocked, vi } from 'vitest';

import { CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

/* eslint-disable @nx/enforce-module-boundaries */

import {
    DotCurrentUserService,
    DotFormatDateService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotPublishingQueueService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { PublishAuditStatus, PublishingJobView } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';

import { DotPublishingQueueShellComponent } from './dot-publishing-queue-shell.component';

import { DotPublishingQueueStore } from '../store/dot-publishing-queue.store';

describe('DotPublishingQueueShellComponent', () => {
    let spectator: Spectator<DotPublishingQueueShellComponent>;
    let dialogService: Mocked<DialogService>;
    let confirmationService: Mocked<ConfirmationService>;
    let store: InstanceType<typeof DotPublishingQueueStore>;

    let onCloseSubject = new Subject<unknown>();
    const dialogRef = {
        close: vi.fn(),
        get onClose() {
            return onCloseSubject;
        }
    } as unknown as DynamicDialogRef;

    const createComponent = createComponentFactory({
        component: DotPublishingQueueShellComponent,
        componentProviders: [
            DotPublishingQueueStore,
            ConfirmationService,
            mockProvider(DialogService, { open: vi.fn().mockReturnValue(dialogRef) })
        ],
        providers: [
            mockProvider(DotPublishingQueueService, {
                listPublishingJobs: vi.fn().mockReturnValue(
                    of({
                        entity: [],
                        pagination: { currentPage: 1, perPage: 10, totalEntries: 0 }
                    })
                ),
                getBundleAssets: vi.fn().mockReturnValue(of([])),
                getPublishingJobDetails: vi.fn().mockReturnValue(of({})),
                probeBundleDownload: vi.fn().mockReturnValue(of(true)),
                probeBundleManifest: vi.fn().mockReturnValue(of(true)),
                getUnsendBundles: vi
                    .fn()
                    .mockReturnValue(of({ identifier: 'id', label: 'name', items: [], numRows: 0 }))
            }),
            mockProvider(DotCurrentUserService, {
                getCurrentUser: vi.fn().mockReturnValue(of({ userId: 'user-1' }))
            }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotFormatDateService),
            mockProvider(DotGlobalMessageService, { error: vi.fn() }),
            mockProvider(DotMessageDisplayService, { push: vi.fn() }),
            mockProvider(DotPushPublishDialogService, { open: vi.fn() }),
            mockProvider(DotDownloadBundleDialogService, { open: vi.fn() }),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA]
    });

    beforeEach(() => {
        vi.clearAllMocks();
        onCloseSubject = new Subject<unknown>();
        spectator = createComponent();
        dialogService = spectator.inject(DialogService, true) as Mocked<DialogService>;
        confirmationService = spectator.inject(
            ConfirmationService,
            true
        ) as Mocked<ConfirmationService>;
        vi.spyOn(confirmationService, 'confirm');
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

        describe('allowRemove per bundle status', () => {
            function openAssetListFor(status: PublishAuditStatus | null): void {
                const row: PublishingJobView = {
                    bundleId: 'B-1',
                    bundleName: 'Bundle 1',
                    status,
                    filterName: null,
                    filterKey: null,
                    assetCount: 2,
                    assetPreview: [],
                    environmentCount: 1,
                    createDate: '2026-01-01T00:00:00Z',
                    statusUpdated: null,
                    numTries: 0
                };
                const service = spectator.inject(DotPublishingQueueService);
                vi.mocked(service.listPublishingJobs).mockReturnValue(
                    of({
                        entity: [row],
                        pagination: { currentPage: 1, perPage: 10, totalEntries: 1 }
                    })
                );
                store.loadBundles();
                store.openAssetList('B-1');
                spectator.detectChanges();
            }

            function allowRemoveOfLastOpen(): boolean {
                const config = dialogService.open.mock.calls.at(-1)?.[1];

                return config?.data?.allowRemove;
            }

            it.each([
                PublishAuditStatus.PUBLISHING_BUNDLE,
                PublishAuditStatus.BUNDLING,
                PublishAuditStatus.SENDING_TO_ENDPOINTS
            ])('opens the asset list read-only for a %s bundle', (status) => {
                openAssetListFor(status);
                expect(allowRemoveOfLastOpen()).toBe(false);
            });

            it.each([
                PublishAuditStatus.BUNDLE_REQUESTED,
                PublishAuditStatus.WAITING_FOR_PUBLISHING,
                PublishAuditStatus.SCHEDULED,
                null
            ])('allows removing assets for a %s bundle', (status) => {
                openAssetListFor(status);
                expect(allowRemoveOfLastOpen()).toBe(true);
            });
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
            vi.spyOn(store, 'deleteBundlesBulk');
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
            const spy = vi.spyOn(store, 'deleteBundlesBulk').mockReturnValue(undefined);
            confirmationService.confirm.mockImplementation((cfg) => {
                cfg.accept?.();
                return confirmationService;
            });
            spectator.component.confirmDeleteBundles();
            expect(spy).toHaveBeenCalledWith(['b1', 'b2']);
        });

        it('does NOT delete on reject', () => {
            store.setBundlesSelection(['b1']);
            const spy = vi.spyOn(store, 'deleteBundlesBulk').mockReturnValue(undefined);
            confirmationService.confirm.mockImplementation((cfg) => {
                cfg.reject?.();
                return confirmationService;
            });
            spectator.component.confirmDeleteBundles();
            expect(spy).not.toHaveBeenCalled();
        });
    });
});
