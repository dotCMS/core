import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA } from '@angular/core';

import { ConfirmationService } from 'primeng/api';

/* eslint-disable @nx/enforce-module-boundaries */

import {
    DotContentTypeService,
    DotCurrentUserService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotPublishingQueueService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';

import { DotPublishingQueueSelectBundleDialogComponent } from './dot-publishing-queue-select-bundle-dialog.component';

const UNSENT_RESPONSE = {
    identifier: 'id',
    label: 'name',
    items: [
        { id: 'bundle-1', name: 'Spring campaign refresh' },
        { id: 'bundle-2', name: 'Blog content sync' }
    ],
    numRows: 2
};

const MOCK_ASSETS = [
    { asset: 'a1', title: 'Spring Sale Landing', type: 'contentlet' },
    { asset: 'a2', title: 'hero-spring.jpg', type: 'contentlet' }
];

describe('DotPublishingQueueSelectBundleDialogComponent', () => {
    let spectator: Spectator<DotPublishingQueueSelectBundleDialogComponent>;
    let service: jest.Mocked<DotPublishingQueueService>;
    let confirmationService: jest.Mocked<ConfirmationService>;
    let pushPublishService: jest.Mocked<DotPushPublishDialogService>;
    let downloadService: jest.Mocked<DotDownloadBundleDialogService>;

    const createComponent = createComponentFactory({
        component: DotPublishingQueueSelectBundleDialogComponent,
        providers: [
            mockProvider(DotPublishingQueueService, {
                getUnsendBundles: jest.fn().mockReturnValue(of(UNSENT_RESPONSE)),
                getBundleAssets: jest.fn().mockReturnValue(of(MOCK_ASSETS)),
                removeAssetsFromBundle: jest
                    .fn()
                    .mockReturnValue(of([{ assetId: 'a1', success: true, message: 'ok' }])),
                deleteBundles: jest.fn().mockReturnValue(of({ entity: 'ok' }))
            }),
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest
                    .fn()
                    .mockReturnValue(of({ userId: 'dotcms.org.1', email: 'admin@dotcms.com' }))
            }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotPushPublishDialogService, { open: jest.fn() }),
            mockProvider(DotDownloadBundleDialogService, { open: jest.fn() }),
            mockProvider(DotContentTypeService, {
                getContentType: jest.fn().mockReturnValue(of({}))
            }),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA]
    });

    beforeEach(() => {
        spectator = createComponent();
        service = spectator.inject(
            DotPublishingQueueService
        ) as jest.Mocked<DotPublishingQueueService>;
        pushPublishService = spectator.inject(
            DotPushPublishDialogService
        ) as jest.Mocked<DotPushPublishDialogService>;
        downloadService = spectator.inject(
            DotDownloadBundleDialogService
        ) as jest.Mocked<DotDownloadBundleDialogService>;
        confirmationService = spectator.inject(
            ConfirmationService,
            true
        ) as jest.Mocked<ConfirmationService>;
        jest.spyOn(confirmationService, 'confirm').mockImplementation((cfg) => {
            cfg.accept?.();
            return confirmationService;
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('init', () => {
        it('fetches drafts via getUnsendBundles and renders both bundle rows', () => {
            spectator.detectChanges();
            expect(service.getUnsendBundles).toHaveBeenCalledWith(
                'dotcms.org.1',
                '*',
                0,
                expect.any(Number)
            );
            expect(spectator.component.bundles().length).toBe(2);
        });

        it('auto-selects the first bundle and loads its assets', () => {
            spectator.detectChanges();
            expect(spectator.component.activeBundleId()).toBe('bundle-1');
            expect(service.getBundleAssets).toHaveBeenCalledWith('bundle-1');
            expect(spectator.component.assets().length).toBe(2);
        });
    });

    describe('select bundle', () => {
        it('clicking a different bundle loads its assets', () => {
            spectator.detectChanges();
            (service.getBundleAssets as jest.Mock).mockClear();

            spectator.component.onSelectBundle({ id: 'bundle-2', name: 'Blog content sync' });
            expect(spectator.component.activeBundleId()).toBe('bundle-2');
            expect(service.getBundleAssets).toHaveBeenCalledWith('bundle-2');
        });

        it('clicking the already-active bundle is a no-op (no extra fetch)', () => {
            spectator.detectChanges();
            (service.getBundleAssets as jest.Mock).mockClear();

            spectator.component.onSelectBundle({ id: 'bundle-1', name: 'Spring campaign refresh' });
            expect(service.getBundleAssets).not.toHaveBeenCalled();
        });
    });

    describe('type icon', () => {
        it('maps known asset types to icons', () => {
            expect(spectator.component.typeIcon('contentlet')).toBe('pi pi-file');
            expect(spectator.component.typeIcon('template')).toBe('pi pi-window-maximize');
        });

        it('falls back to a generic icon for unknown types', () => {
            expect(spectator.component.typeIcon('weird-type')).toBe('pi pi-file');
        });
    });

    describe('remove asset', () => {
        it('confirms then calls removeAssetsFromBundle and refetches', () => {
            spectator.detectChanges();
            (service.getBundleAssets as jest.Mock).mockClear();

            spectator.component.onRemoveAsset({
                asset: 'a1',
                title: 'Spring Sale Landing',
                type: 'contentlet'
            });

            expect(confirmationService.confirm).toHaveBeenCalled();
            expect(service.removeAssetsFromBundle).toHaveBeenCalledWith('bundle-1', ['a1']);
            expect(service.getBundleAssets).toHaveBeenCalledWith('bundle-1');
        });

        it('is a no-op when no active bundle', () => {
            spectator.detectChanges();
            spectator.component.activeBundleId.set(null);
            (service.removeAssetsFromBundle as jest.Mock).mockClear();
            spectator.component.onRemoveAsset({
                asset: 'a1',
                title: 'x',
                type: 'contentlet'
            });
            expect(service.removeAssetsFromBundle).not.toHaveBeenCalled();
        });

        it('on service error: hands off to httpErrorManager', () => {
            spectator.detectChanges();
            const error = new Error('boom');
            (service.removeAssetsFromBundle as jest.Mock).mockReturnValueOnce(
                throwError(() => error)
            );
            const handler = spectator.inject(
                DotHttpErrorManagerService
            ) as jest.Mocked<DotHttpErrorManagerService>;
            spectator.component.onRemoveAsset({
                asset: 'a1',
                title: 'x',
                type: 'contentlet'
            });
            expect(handler.handle).toHaveBeenCalledWith(error);
        });
    });

    describe('remove bundles (bulk)', () => {
        it('confirms then calls deleteBundles with the checked ids; auto-selects next bundle if active was deleted', () => {
            spectator.detectChanges();
            spectator.component.onCheckedChange([
                { id: 'bundle-1', name: 'Spring campaign refresh' }
            ]);
            // After delete, the next list call returns only the remaining bundle.
            (service.getUnsendBundles as jest.Mock).mockReturnValueOnce(
                of({
                    identifier: 'id',
                    label: 'name',
                    items: [{ id: 'bundle-2', name: 'Blog content sync' }],
                    numRows: 1
                })
            );

            spectator.component.onRemoveBundles();

            expect(confirmationService.confirm).toHaveBeenCalled();
            expect(service.deleteBundles).toHaveBeenCalledWith(['bundle-1']);
            // Active flips off the deleted bundle and re-selects the next remaining one.
            expect(spectator.component.activeBundleId()).toBe('bundle-2');
        });

        it('is a no-op when nothing is checked', () => {
            spectator.detectChanges();
            (service.deleteBundles as jest.Mock).mockClear();
            spectator.component.onRemoveBundles();
            expect(service.deleteBundles).not.toHaveBeenCalled();
        });
    });

    describe('configure / download', () => {
        it('Configure → opens push publish dialog for the active bundle', () => {
            spectator.detectChanges();
            spectator.component.onConfigureActive();
            expect(pushPublishService.open).toHaveBeenCalledWith(
                expect.objectContaining({
                    assetIdentifier: 'bundle-1',
                    isBundle: true
                })
            );
        });

        it('Download → opens download dialog for the active bundle', () => {
            spectator.detectChanges();
            spectator.component.onDownloadActive();
            expect(downloadService.open).toHaveBeenCalledWith('bundle-1');
        });

        it('Configure / Download are no-ops when no active bundle', () => {
            spectator.detectChanges();
            spectator.component.activeBundleId.set(null);
            (pushPublishService.open as jest.Mock).mockClear();
            (downloadService.open as jest.Mock).mockClear();
            spectator.component.onConfigureActive();
            spectator.component.onDownloadActive();
            expect(pushPublishService.open).not.toHaveBeenCalled();
            expect(downloadService.open).not.toHaveBeenCalled();
        });
    });

    describe('layout', () => {
        it('renders the two panes + action bar', () => {
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-select-bundle-left'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-select-bundle-right'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-select-bundle-actions'))).toBeTruthy();
        });

        it('renders bundle rows', () => {
            spectator.detectChanges();
            expect(spectator.queryAll(byTestId('pq-select-bundle-row')).length).toBe(2);
        });
    });
});
