import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Subject, of } from 'rxjs';

import { CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA } from '@angular/core';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import {
    DotEventsService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotPublishingQueueService,
    DotPushPublishFiltersService,
    DotSiteService
} from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueueShellComponent } from './dot-publishing-queue-shell.component';

import { DotPublishingQueueStore } from '../dot-publishing-queue-page/store/dot-publishing-queue.store';

describe('DotPublishingQueueShellComponent', () => {
    let spectator: Spectator<DotPublishingQueueShellComponent>;
    let dialogService: jest.Mocked<DialogService>;
    let store: InstanceType<typeof DotPublishingQueueStore>;

    const onCloseSubject = new Subject<unknown>();
    const dialogRef = {
        close: jest.fn(),
        onClose: onCloseSubject
    } as unknown as DynamicDialogRef;

    const createComponent = createComponentFactory({
        component: DotPublishingQueueShellComponent,
        componentProviders: [
            DotPublishingQueueStore,
            mockProvider(DialogService, { open: jest.fn().mockReturnValue(dialogRef) })
        ],
        providers: [
            mockProvider(DotPublishingQueueService, {
                listPublishingJobs: jest
                    .fn()
                    .mockReturnValue(
                        of({
                            entity: [],
                            pagination: { currentPage: 1, perPage: 10, totalEntries: 0 }
                        })
                    ),
                getBundleAssets: jest.fn().mockReturnValue(of([])),
                getPublishingJobDetails: jest.fn().mockReturnValue(of({})),
                getEnvironments: jest.fn().mockReturnValue(of([]))
            }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotPushPublishFiltersService, { get: jest.fn().mockReturnValue(of([])) }),
            mockProvider(DotEventsService, { listen: jest.fn().mockReturnValue(of({})) }),
            mockProvider(DotSiteService, {
                getSites: jest.fn().mockReturnValue(of({ sites: [], total: 0 }))
            }),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA]
    });

    beforeEach(() => {
        jest.clearAllMocks();
        spectator = createComponent();
        dialogService = spectator.inject(DialogService, true) as jest.Mocked<DialogService>;
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

    describe('configure & send dialog sync', () => {
        it('opens dialog when pushBundleTarget becomes set', () => {
            store.openConfigureSend({
                bundleId: 'B-3',
                bundleName: 'Bundle 3'
            } as Parameters<typeof store.openConfigureSend>[0]);
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
});
