import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { signal } from '@angular/core';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService, DotPushPublishFiltersService } from '@dotcms/data-access';
import { PublishAuditStatus, PublishingJobView } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueueConfigureSendDialogComponent } from './dot-publishing-queue-configure-send-dialog.component';

import { DotPublishingQueueStore } from '../../dot-publishing-queue-page/store/dot-publishing-queue.store';

const bundleFixture: PublishingJobView = {
    bundleId: 'b-1',
    bundleName: 'Bundle 1',
    status: PublishAuditStatus.WAITING_FOR_PUBLISHING,
    filterName: null,
    filterKey: null,
    assetCount: 1,
    assetPreview: [],
    environmentCount: 0,
    createDate: '2026-06-08T10:00:00Z',
    statusUpdated: null,
    numTries: 0
};

describe('DotPublishingQueueConfigureSendDialogComponent', () => {
    let spectator: Spectator<DotPublishingQueueConfigureSendDialogComponent>;
    let store: ReturnType<typeof makeStoreStub>;
    let dialogRef: jest.Mocked<DynamicDialogRef>;

    const pushBundleTarget = signal<PublishingJobView | null>(null);
    const pushInFlight = signal(false);
    const environments = signal([
        { id: 'env-1', name: 'Prod' },
        { id: 'env-2', name: 'Staging' }
    ]);

    function makeStoreStub() {
        return {
            pushBundleTarget,
            pushInFlight,
            environments,
            submitPush: jest.fn((_id, _payload, cb: () => void) => cb())
        };
    }

    const createComponent = createComponentFactory({
        component: DotPublishingQueueConfigureSendDialogComponent,
        providers: [
            mockProvider(DotPublishingQueueStore, makeStoreStub()),
            mockProvider(DotPushPublishFiltersService, {
                get: jest.fn().mockReturnValue(
                    of([
                        { defaultFilter: true, key: 'default.yml', title: 'Default' },
                        { defaultFilter: false, key: 'force.yml', title: 'Force Push' }
                    ])
                )
            }),
            mockProvider(DynamicDialogRef, { close: jest.fn() }),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ]
    });

    beforeEach(() => {
        pushBundleTarget.set(bundleFixture);
        pushInFlight.set(false);
        spectator = createComponent();
        store = spectator.inject(DotPublishingQueueStore) as unknown as ReturnType<
            typeof makeStoreStub
        >;
        dialogRef = spectator.inject(DynamicDialogRef) as jest.Mocked<DynamicDialogRef>;
        jest.clearAllMocks();
    });

    it('starts with operation=push and scheduleMode=now', () => {
        expect(spectator.component.operation()).toBe('push');
        expect(spectator.component.scheduleMode()).toBe('now');
    });

    it('canSubmit is false with no env selected', () => {
        expect(spectator.component.canSubmit()).toBe(false);
    });

    it('canSubmit becomes true with an env and a filter selected', () => {
        spectator.component.selectedEnvironments.set(['env-1']);
        spectator.component.selectedFilterKey.set('default.yml');
        expect(spectator.component.canSubmit()).toBe(true);
    });

    it('hides filter dropdown for remove operation, and submits without filter', () => {
        spectator.component.setOperation('remove');
        spectator.component.selectedEnvironments.set(['env-1']);
        spectator.component.scheduleMode.set('schedule');
        spectator.component.expireDate.set(new Date('2026-07-01T00:00:00'));
        expect(spectator.component.canSubmit()).toBe(true);

        spectator.component.onSubmit();
        expect(store.submitPush).toHaveBeenCalledWith(
            'b-1',
            expect.objectContaining({
                operation: 'expire',
                publishDate: undefined,
                filterKey: expect.any(String)
            }),
            expect.any(Function)
        );
    });

    it('maps design operation push → publish on submit', () => {
        spectator.component.selectedEnvironments.set(['env-1']);
        spectator.component.selectedFilterKey.set('default.yml');
        spectator.component.onSubmit();
        expect(store.submitPush).toHaveBeenCalledWith(
            'b-1',
            expect.objectContaining({ operation: 'publish' }),
            expect.any(Function)
        );
    });

    it('maps pushremove → publishexpire', () => {
        spectator.component.setOperation('pushremove');
        spectator.component.selectedEnvironments.set(['env-1']);
        spectator.component.selectedFilterKey.set('default.yml');
        spectator.component.expireDate.set(new Date('2026-08-01T00:00:00'));
        spectator.component.onSubmit();
        expect(store.submitPush).toHaveBeenCalledWith(
            'b-1',
            expect.objectContaining({ operation: 'publishexpire' }),
            expect.any(Function)
        );
    });

    it('cancel closes the dialog without submitting', () => {
        spectator.component.onCancel();
        expect(dialogRef.close).toHaveBeenCalled();
        expect(store.submitPush).not.toHaveBeenCalled();
    });
});
