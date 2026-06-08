import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Subject } from 'rxjs';

import { signal } from '@angular/core';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueuePageComponent } from './dot-publishing-queue-page.component';
import { DotPublishingQueueStore } from './store/dot-publishing-queue.store';

describe('DotPublishingQueuePageComponent', () => {
    let spectator: Spectator<DotPublishingQueuePageComponent>;
    let dialogService: jest.Mocked<DialogService>;
    let store: InstanceType<typeof DotPublishingQueueStore>;

    const selectedBundleId = signal<string | null>(null);
    const onCloseSubject = new Subject<unknown>();
    const dialogRefStub = {
        close: jest.fn(),
        onClose: onCloseSubject
    } as unknown as DynamicDialogRef;

    const createComponent = createComponentFactory({
        component: DotPublishingQueuePageComponent,
        componentProviders: [
            mockProvider(DotPublishingQueueStore, {
                readyRows: jest.fn().mockReturnValue([]),
                readyStatus: jest.fn().mockReturnValue('loaded'),
                readyTotal: jest.fn().mockReturnValue(0),
                readyPage: jest.fn().mockReturnValue(1),
                rowsPerPage: jest.fn().mockReturnValue(10),
                progressRows: jest.fn().mockReturnValue([]),
                progressStatus: jest.fn().mockReturnValue('loaded'),
                progressTotal: jest.fn().mockReturnValue(0),
                progressPage: jest.fn().mockReturnValue(1),
                selectedBundleId: selectedBundleId,
                openAssetList: jest.fn(),
                setReadyPage: jest.fn(),
                setProgressPage: jest.fn(),
                closeAssetList: jest.fn()
            })
        ],
        providers: [
            mockProvider(DialogService, { open: jest.fn().mockReturnValue(dialogRefStub) }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'publishing-queue.asset-list.title': 'Bundle Assets',
                    'publishing-queue.ready.title': 'Ready',
                    'publishing-queue.in-progress.title': 'In Progress',
                    'publishing-queue.empty.ready': 'Empty',
                    'publishing-queue.empty.in-progress': 'Nothing in progress'
                })
            }
        ]
    });

    beforeEach(() => {
        selectedBundleId.set(null);
        spectator = createComponent();
        dialogService = spectator.inject(DialogService, true) as jest.Mocked<DialogService>;
        store = spectator.inject(DotPublishingQueueStore, true);
        jest.clearAllMocks();
        (dialogRefStub.close as jest.Mock).mockClear();
    });

    it('renders both ready and progress list slots', () => {
        expect(spectator.query(byTestId('pq-ready-list'))).toBeTruthy();
        expect(spectator.query(byTestId('pq-progress-list'))).toBeTruthy();
    });

    it('opens dialog when selectedBundleId becomes set', () => {
        selectedBundleId.set('bundle-7');
        spectator.detectChanges();

        expect(dialogService.open).toHaveBeenCalled();
        const config = (dialogService.open as jest.Mock).mock.calls[0][1];
        expect(config.width).toBe('700px');
        expect(config.closable).toBe(true);
        expect(config.closeOnEscape).toBe(true);
    });

    it('calls store.closeAssetList when dialog emits onClose', () => {
        selectedBundleId.set('bundle-7');
        spectator.detectChanges();

        onCloseSubject.next(undefined);

        expect(store.closeAssetList).toHaveBeenCalled();
    });

    it('closes the open dialog when selectedBundleId becomes null', () => {
        selectedBundleId.set('bundle-7');
        spectator.detectChanges();
        selectedBundleId.set(null);
        spectator.detectChanges();

        expect(dialogRefStub.close).toHaveBeenCalled();
    });
});
